> [!IMPORTANT]
> - If you encounter any issues while trying to set up the project initially, the bottom of this README has a troubleshooting section detailing solutions to common issues.
> - Additional documentation can be found in the `docs` folder.

## WPILib Installation

For a more comprehensive installation guide, visit the [WPILib docs](https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html). Otherwise, follow the appropriate guide below. All installers can be found on [the latest GitHub WPILib release](https://github.com/wpilibsuite/allwpilib/releases/).

### Windows

1. Scroll down to the downloads section of the latest release and select the Windows option.
2. Extract the installer `.iso` file by right-clicking it and selecting `Mount` or by installing [7-Zip](https://www.7-zip.org/download.html) and using the `7-Zip > Extract to WPILib_Windows64-<VERSION_DATE>\` option.
3. Run the `WPILibInstaller.exe`.
4. Follow the general instructions below

### Linux

1. Download the `Linux (x64)` file in the latest release.
2. Run the following commands in the folder you downloaded the file to

```sh
tar -xf WPILib_Linux-<version>.tar.gz
cd WPILib_Linux-<version>/
./WPILibInstaller
```

_Post-installation ***note***: Some Linux distributions will require you to give opening permissions for `.desktop` files. To give these permissions, simply run `chmod a+x <PATH_TO_DESKTOP_FILE>`. To start the application, some distributions like Ubuntu have built-in launchers (opened by pressing `super`); otherwise, install a program like [rofi](https://github.com/davatorium/rofi)._

### General

- When you are asked to select the install mode you would like, select `Everything`.
- The installer will ask you how you would like to install/handle the VSCode installation. It is easiest if you select `Download for this computer only (fastest)`, regardless of your OS or distro.
- This year, we will be using Java for all robot programming. Please follow the Java installation guide on your newly installed WPILib VSCode IDE.

## Java Installation

> [!NOTE]
> The following Java installation instructions assume that you are using the WPILib provided version of VSCode. Other IDEs will have a fairly similar installation process, but they will not be covered here.

### Windows

Visit the [installation page](https://learn.microsoft.com/en-us/java/openjdk/download) and use the `.msi` installer to download the JDK. Open up your terminal and try the following commands to identify the JDK installation location:

```
where javac
echo %JAVA_HOME%
```

One of the commands should output something like `C:\Program Files\Java\jdk-17\bin\javac.exe` or `C:\Program Files\OpenJDK\jdk-17`. Only include the part up to `jdk-17` when putting your Java path in VSCode. Finally, in your VSCode `settings.json` (`Ctrl+Shift+P` then type and select the option `Preferences: Open User Settings (JSON)`), include the following line with the appropriate JDK installation path.

```json
  "java.home": "<JDK_PATH>",
```

### Linux

#### Install the JDK:
As of now, WPILib primarily supports Java 17, so we will have to explicitly install an older version of Java.

Arch: `yay -S jdk17-openjdk` (or your AUR helper of choice)

Debian: `sudo apt-get install openjdk-17-jdk`

#### Identify the installation location and link the path
Assuming you only have one JDK installed, the command `dirname $(dirname $(readlink -f $(which javac)))` will output the location of your JDK. It should look something like `/usr/lib/jvm/java-17-openjdk`. In that case, you are safe to just put the following into your `settings.json` file (`Ctrl+Shift+P` then type and select the option `Preferences: Open User Settings (JSON)`).

```json
  "java.home": "<JDK_PATH>",
```

If you already have a different version of Java installed, and the previous command printed something like `/usr/lib/jvm/java-24-jdk`, you can also add the following to your config so the integrated terminal uses the correct Java version. If, for some reason, you installed a Java 17 JDK from a different source, more often than not, you can find your JDK path by running `ls /usr/lib/jvm`.

```json
  "terminal.integrated.env.linux": {
    "JAVA_HOME": "<JDK_PATH>",
    "PATH": "<JDK_PATH>/bin:${env:PATH}"
  },
```

If you ever plan on running commands for FRC outside of the WPILib integrated terminal, you can add the following to your shell configuration file (probably `.bashrc` in your `$HOME` directory).

```sh
  export JAVA_HOME="<JDK_PATH>"
  export PATH="$JAVA_HOME/bin:$PATH"
```

#### Verify your installation
Run `java -version` to verify that you have properly installed Java and that it is using version 17.

## Tools

### AdvantageKit
Logging is a key debugging strategy for understanding what went wrong in your code or how to make improvements. AdvantageKit serves as a comprehensive logging tool that enables you to collect all sensor data, inputs, and outputs so you can replay what happened in a match by using a simulation. The [AdvantageKit manual swerve drive with Talon FX](https://docs.advantagekit.org/getting-started/template-projects/talonfx-swerve-template/) serves as our base swerve implementation.

### CTRE Swerve API
Our base swerve code interacts with the CTRE Swerve API using 4 core classes:
- `SwerveDrivetrainConstants`: Handles characteristics of the robot that are not module specific(CAN bus, Pigeon 2 ID, etc).
- `SwerveModuleConstantsFactory`: Factory class that is used to instantiate `SwerveModuleConstants` for each module on the robot.
- `SwerveModuleConstants`: Represents the characteristics for a given module.
- `SwerveDrivetrain`: Created using `SwerveDrivetrainConstants` and a `SwerveModuleConstants` for each module. Used to control the swerve drivetrain.

For more information,n view the [swerve api article](https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/mechanisms/swerve/swerve-builder-api.html) and the [swerve requests article](https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/mechanisms/swerve/swerve-builder-api.html).

### MapleSim
Although less useful once we have access to a physical robot, MapleSim is a powerful physics simulation tool that can emulate robot movement and interaction with game pieces. More details on the simulation integration with CTRE Swerve and Advantage Kit can be found [here](https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/simulation/simulation-intro.html).

### Choreo
The first 15 seconds of every FRC game involve an autonomous phase. Scoring as many points as possible during this phase is important because there are point bonuses, and you don't have any defensive robots attempting to stop you from scoring. Choreo is a path planning tool that uses an intuitive GUI. Beyond creating simple paths, combining this movement with automatic scoring and chaining together auton paths depending on the robot's location and whether or not points were successfully scored, there are a multitude of ways to make an effective autonomous system. See more documentation on creating autos on the [docs](https://choreo.autos/choreolib/auto-factory/).

To install Choreo, visit the [GitHub releases page](https://github.com/SleipnirGroup/Choreo/releases) and scroll down to Assets. If you are on **Windows**, use the `Choreo-<VERSION_NUMBER>-Windows-x86_64-setup.exe` installer. If you are on a **Debian** based Linux distro (e.g., Ubuntu), download the `Choreo-<VERSION_NUMBER>-Linux-x86_64.deb` installer, switch to the directory you installed the file in, and run `sudo apt install ./Choreo-<VERSION_NUMBER>-Linux-x86_64.deb`. If you are on an **Arch** based distro, simply run `yay -S slepinirgroup-choreo-bin` (or use your AUR helper of choice).

### PhotonVision
Most FRC games will have some form of reflective tape or AprilTags that can be easily identified by using computer vision (CV). Not only does CV help us generate accurate odometry and localize our robot, but it can also help create features like autoaim. In tandem with command-based programming, we can use AprilTag detections to line up the robot to enhance the drivers ability to score.

PhotonVision requires a coprocessor to run CV. We will likely use the OrangePi or RaspberryPi with a simple networking setup to interface with the other code and subsystems using the RoboRio.

All installation details, API documentation, and code examples can be found on the [PhotonVision documentation](https://docs.photonvision.org/en/latest/index.html).

> [!TIP]
> After cloning the repo, all vendor dependencies should be automatically installed after building with `./gradlew build`, but if they aren't installed, all of them can be found by hitting `Ctrl+Shift+P` and selecting the `View: Show WPILib Vendor Dependencies` option.

### Git
Git is a version management tool. The platform that this repository is hosted on, GitHub, is based on Git. In order to make storing, sharing, and contributing to the project easier, all code for the robot will be managed using Git.

Below are some resources for learning Git.
- [Oh My Git (Game)](https://ohmygit.org/)
- [Git - The Simple Guide](https://rogerdudler.github.io/git-guide/)
- [Git Explained in 100 Seconds](https://www.youtube.com/watch?v=hwP7WQkmECE&t=9s)

To clone this repo, run:
```sh
git clone https://github.com/RedshiftRobotics3676/2026-Redshift-Base.git
```

After you make changes, commit them by running:
```sh
git add . # Adds all files, target a specific file with 'git add <path/to/file>'
git commit -m "Your very cool message about how you contributed to the project"
```

Finally, if no changes were made to the remote repository (by other people contributing to the GitHub repo), you can run:
```sh
git push
```

If you haven't already configured your user, install the [GitHub CLI](https://cli.github.com/) and run the following:
```sh
git config --global user.name "<YOUR_GITHUB_USERNAME>"
git config --global user.email "<YOUR_GITHUB_EMAIL>"
gh auth login # Select HTTPS and login through your web browser
```

If someone did happen to make changes, try pulling the changes with a rebase (only works if you changed separate, unrelated parts of the project).
```sh
git config --global pull.rebase true
git pull
```

Otherwise, if there are merge conflicts, `git status` to identify all conflicting files, resolve the conflicts, stage the changes, and commit them again. Find more details on merging [here](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/addressing-merge-conflicts/resolving-a-merge-conflict-using-the-command-line).

## Troubleshooting

### Gradle Build Issues
9 times out of 10, any nasty Gradle build errors will happen if you have the wrong version of Java installed, or you added new packages incorrectly. The easiest way to check if you have the wrong Java version installed is to run `java -version` in the terminal you are using. If it displays anything other than version 17, refer back to the [Java Installation](#java-installation) section. Otherwise, look at previous commits of the `build.gradle` file to confirm no breaking changes have been made. If none of these things solve the problem, run the Gradle command with the `-s` option to see a more detailed overview of the errors.
