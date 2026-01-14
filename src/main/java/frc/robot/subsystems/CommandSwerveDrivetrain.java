package frc.robot.subsystems;

import static edu.wpi.first.units.Units.*;

import choreo.Choreo.TrajectoryLogger;
import choreo.auto.AutoFactory;
import choreo.trajectory.SwerveSample;
import com.ctre.phoenix6.SignalLogger;
import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.swerve.SwerveRequest;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Notifier;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.constants.TunerConstants.TunerSwerveDrivetrain;
import java.util.function.Supplier;

public class CommandSwerveDrivetrain extends TunerSwerveDrivetrain implements Subsystem {
  private static final double kSimLoopPeriod = 0.005; // ms
  private Notifier simNotifier = null;
  private double lastSimTime;

  private static final Rotation2d kBlueAllianceForwardOrientation = Rotation2d.kZero;
  private static final Rotation2d kRedAllianceForwardOrientation = Rotation2d.k180deg;
  private boolean hasAppliedOperatorPerspective = false; // Operator perspective toggle

  // Path following swerve requests
  private final SwerveRequest.ApplyFieldSpeeds pathApplyFieldSpeeds =
      new SwerveRequest.ApplyFieldSpeeds();
  private final PIDController pathXController = new PIDController(10, 0, 0);
  private final PIDController pathYController = new PIDController(10, 0, 0);
  private final PIDController pathThetaController = new PIDController(7, 0, 0);

  // SysId characterization swerve requests
  private final SwerveRequest.SysIdSwerveTranslation translationCharacterization =
      new SwerveRequest.SysIdSwerveTranslation();
  private final SwerveRequest.SysIdSwerveSteerGains steerCharacterization =
      new SwerveRequest.SysIdSwerveSteerGains();
  private final SwerveRequest.SysIdSwerveRotation rotationCharacterization =
      new SwerveRequest.SysIdSwerveRotation();

  // SysId routine for characterizing translation to find drive motor PID gains
  private final SysIdRoutine sysIdRoutineTranslation =
      new SysIdRoutine(
          new SysIdRoutine.Config(
              null, // 1 V/s default
              Volts.of(4), // Reduce dynamic step voltage to 4 V to prevent brownout
              null, // 10s default
              state -> SignalLogger.writeString("SysIdTranslation_State", state.toString())),
          new SysIdRoutine.Mechanism(
              output -> setControl(translationCharacterization.withVolts(output)), null, this));

  // SysId routine for characterizing steer to find steer motor PID gains
  private final SysIdRoutine sysIdRoutineSteer =
      new SysIdRoutine(
          new SysIdRoutine.Config(
              null, // 1 V/s default
              Volts.of(7), // Use dynamic voltage of 7 V
              null, // 10s default
              state -> SignalLogger.writeString("SysIdSteer_State", state.toString())),
          new SysIdRoutine.Mechanism(
              volts -> setControl(steerCharacterization.withVolts(volts)), null, this));

  /*
   * SysId routine for characterizing rotation to find PID gains for the FieldCentricFacingAngle HeadingController.
   * See the documentation of SwerveRequest.SysIdSwerveRotation for info on importing the log to SysId.
   */
  private final SysIdRoutine sysIdRoutineRotation =
      new SysIdRoutine(
          new SysIdRoutine.Config(
              Volts.of(Math.PI / 6).per(Second), // rad/s^2 SysId only supports V/s
              Volts.of(Math.PI), // rad/s; SysId only supports V
              null, // 10s default
              state -> SignalLogger.writeString("SysIdRotation_State", state.toString())),
          new SysIdRoutine.Mechanism(
              output -> {
                setControl(rotationCharacterization.withRotationalRate(output.in(Volts)));
                SignalLogger.writeDouble("Rotational_Rate", output.in(Volts));
              },
              null,
              this));

  private SysIdRoutine sysIdRoutineToApply = sysIdRoutineTranslation;

  public CommandSwerveDrivetrain(
      SwerveDrivetrainConstants drivetrainConstants, SwerveModuleConstants<?, ?, ?>... modules) {
    super(drivetrainConstants, modules);
    if (Utils.isSimulation()) {
      startSimThread();
    }
  }

  public CommandSwerveDrivetrain(
      SwerveDrivetrainConstants drivetrainConstants,
      double odometryUpdateFrequency, // 250 Hz on CAN FD and 100 Hz on CAN 2.0
      SwerveModuleConstants<?, ?, ?>... modules) {
    super(drivetrainConstants, odometryUpdateFrequency, modules);
    if (Utils.isSimulation()) {
      startSimThread();
    }
  }

  // Standard deviations: [x, y, theta]ᵀ  units: [m, m, rad]
  public CommandSwerveDrivetrain(
      SwerveDrivetrainConstants drivetrainConstants,
      double odometryUpdateFrequency,
      Matrix<N3, N1> odometryStandardDeviation,
      Matrix<N3, N1> visionStandardDeviation,
      SwerveModuleConstants<?, ?, ?>... modules) {
    super(
        drivetrainConstants,
        odometryUpdateFrequency,
        odometryStandardDeviation,
        visionStandardDeviation,
        modules);
    if (Utils.isSimulation()) {
      startSimThread();
    }
  }

  public AutoFactory createAutoFactory() {
    return createAutoFactory((sample, isStart) -> {});
  }

  public AutoFactory createAutoFactory(TrajectoryLogger<SwerveSample> trajLogger) {
    return new AutoFactory(
        () -> getState().Pose, this::resetPose, this::followPath, true, this, trajLogger);
  }

  public Command applyRequest(Supplier<SwerveRequest> requestSupplier) {
    return run(() -> this.setControl(requestSupplier.get()));
  }

  public void followPath(SwerveSample sample) {
    pathThetaController.enableContinuousInput(-Math.PI, Math.PI);

    var pose = getState().Pose;

    var targetSpeeds = sample.getChassisSpeeds();
    targetSpeeds.vxMetersPerSecond += pathXController.calculate(pose.getX(), sample.x);
    targetSpeeds.vyMetersPerSecond += pathYController.calculate(pose.getY(), sample.y);
    targetSpeeds.omegaRadiansPerSecond +=
        pathThetaController.calculate(pose.getRotation().getRadians(), sample.heading);

    setControl(
        pathApplyFieldSpeeds
            .withSpeeds(targetSpeeds)
            .withWheelForceFeedforwardsX(sample.moduleForcesX())
            .withWheelForceFeedforwardsY(sample.moduleForcesY()));
  }

  public Command sysIdQuasistatic(SysIdRoutine.Direction direction) {
    return sysIdRoutineToApply.quasistatic(direction);
  }

  public Command sysIdDynamic(SysIdRoutine.Direction direction) {
    return sysIdRoutineToApply.dynamic(direction);
  }

  @Override
  public void periodic() {
    /*
     * Try to apply the operator perspective
     * If not applied the before, then apply it regardless of DS state
     * Allows us to correct the perspective in case the robot code restarts mid-match
     */
    if (!hasAppliedOperatorPerspective || DriverStation.isDisabled()) {
      DriverStation.getAlliance()
          .ifPresent(
              allianceColor -> {
                setOperatorPerspectiveForward(
                    allianceColor == Alliance.Red
                        ? kRedAllianceForwardOrientation
                        : kBlueAllianceForwardOrientation);
                hasAppliedOperatorPerspective = true;
              });
    }
  }

  private void startSimThread() {
    lastSimTime = Utils.getCurrentTimeSeconds();

    // Run simulation faster to make PID behave properly
    simNotifier =
        new Notifier(
            () -> {
              final double currentTime = Utils.getCurrentTimeSeconds();
              double deltaTime = currentTime - lastSimTime;
              lastSimTime = currentTime;

              updateSimState(deltaTime, RobotController.getBatteryVoltage());
            });
    simNotifier.startPeriodic(kSimLoopPeriod);
  }

  // Additional Kalman Filter odom input
  @Override
  public void addVisionMeasurement(Pose2d visionRobotPoseMeters, double timestampSeconds) {
    super.addVisionMeasurement(visionRobotPoseMeters, Utils.fpgaToCurrentTime(timestampSeconds));
  }

  // Alternate method to account for noise
  @Override
  public void addVisionMeasurement(
      Pose2d visionRobotPoseMeters,
      double timestampSeconds,
      Matrix<N3, N1> visionMeasurementStdDevs) {
    super.addVisionMeasurement(
        visionRobotPoseMeters, Utils.fpgaToCurrentTime(timestampSeconds), visionMeasurementStdDevs);
  }
}
