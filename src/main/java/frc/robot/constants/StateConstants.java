package frc.robot.constants;

import edu.wpi.first.wpilibj.RobotBase;

public class StateConstants {
  public static final Mode simMode = Mode.SIM;
  public static final Mode currentMode = RobotBase.isReal() ? Mode.REAL : simMode;

  public static enum Mode {
    REAL,
    SIM,
    REPLAY
  }
}
