package org.firstinspires.ftc.teamcode.opmodes;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.subsystems.complex.ComplexActions;
import org.firstinspires.ftc.teamcode.subsystems.lift.LiftActions;

import com.arcrobotics.ftclib.controller.PIDController;
import com.qualcomm.robotcore.hardware.Servo;


@Config
@TeleOp(name = "PrimaryOpMode")
public class PrimaryOpMode extends LinearOpMode {

    // private FtcDashboard dash = FtcDashboard.getInstance();
    // private List<Action> runningActions = new ArrayList<>();

    public static class Params {
        public double speedMult      = 0.5;
        public double turnMult       = 1;

        public double backMotorMult  = 1;
        public double frontMotorMult = 1;

        public double kP             = 2;
        public double kI             = 0.1;
        public double kD             = 0;

        public double power        = 1;

        public double clawServoAmount = 0.2;
        public int armTicks = 3850;
        public double leftMotorMult = 1;
        public double rightMotorMult = 1;
    }
    public static Params PARAMS = new Params();

    @Override
    public void runOpMode() throws InterruptedException {
        // TelemetryPacket packet = new TelemetryPacket();

        DcMotor frontLeftMotor         = hardwareMap.dcMotor.get("frontLeft");
        DcMotor backLeftMotor          = hardwareMap.dcMotor.get("backLeft");
        DcMotor frontRightMotor        = hardwareMap.dcMotor.get("frontRight");
        DcMotor backRightMotor         = hardwareMap.dcMotor.get("backRight");

        DcMotor leftElevator  = hardwareMap.dcMotor.get("armLeft");
        DcMotor rightElevator = hardwareMap.dcMotor.get("armRight");
        DcMotor frontArm      = hardwareMap.dcMotor.get("frontArm");
        DcMotor spinner       = hardwareMap.dcMotor.get("spinner");

        Servo rotator         = hardwareMap.servo.get("rotatorServo");
        Servo clawServo       = hardwareMap.servo.get("clawServo");

        CRServo leftSpinArm   = hardwareMap.crservo.get("leftSpinner");
        CRServo leftSlide       = hardwareMap.crservo.get("leftSlider");
        CRServo rightSlide      = hardwareMap.crservo.get("rightSlider");

        DcMotor leftDrive  = hardwareMap.get(DcMotor.class, "armLeft");
        DcMotor rightDrive = hardwareMap.get(DcMotor.class, "armRight");
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        // rightDrive.setDirection(DcMotorSimple.Direction.REVERSE);
        leftDrive.setDirection(DcMotorSimple.Direction.REVERSE);

        // make the motors brake when [power == 0]
        // should stop the elevator from retracting because of gravity...
        rightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        int rightStartingPos = rightDrive.getCurrentPosition() + 250;
        int leftStartingPos = leftDrive.getCurrentPosition() + 250;
        int frontStartingPos = frontArm.getCurrentPosition();

        rightDrive.setTargetPosition(rightStartingPos);
        leftDrive.setTargetPosition(leftStartingPos);
        rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftDrive.setPower(1);
        rightDrive.setPower(1);

        // Reset the motor encoder so that it reads zero ticks
        frontLeftMotor      .setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeftMotor       .setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightMotor     .setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRightMotor      .setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);


        // Turn the motor back on, required if you use STOP_AND_RESET_ENCODER
        frontLeftMotor      .setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeftMotor       .setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRightMotor     .setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRightMotor      .setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        frontLeftMotor      .setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeftMotor       .setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRightMotor     .setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRightMotor      .setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        //For manual control without Actions

        // Reverse the right side motors. This may be wrong for your setup.
        // If your robot moves backwards when commanded to go forwards,
        // reverse the left side instead.
        // See the note about this earlier on this page.
        frontLeftMotor      .setDirection(DcMotorSimple.Direction.REVERSE);
        backLeftMotor       .setDirection(DcMotorSimple.Direction.REVERSE);

        leftSpinArm         .setDirection(DcMotorSimple.Direction.FORWARD);

        // Retrieve the IMU from the hardware map
        IMU imu = hardwareMap.get(IMU.class, "imu");

        // Adjust the orientation parameters to match your robot
        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.RIGHT));

        // Without this, the REV Hub's orientation is assumed to be logo up / USB forward
        imu.initialize(parameters);
        imu.resetYaw();

        PIDController pid = new PIDController(PARAMS.kP, PARAMS.kI, PARAMS.kD);

        waitForStart();

        if (isStopRequested()) return;

        double wantedAngle = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
        pid.setSetPoint(wantedAngle);
        boolean isTurning  = false;
        double botHeading  = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);

        boolean isSpinnerSpinning = false;
        boolean isSpinArmDown = false;

        while (opModeIsActive()) {
            /* ##################################################
                            Inputs and Initializing
               ################################################## */

            double currentHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
            botHeading = unwrapAngle(botHeading, currentHeading)%(Math.PI*2); // Use unwrapping here

            double y  = -gamepad1.left_stick_y; // Remember, Y stick value is reversed
            double x  = 1.1 * gamepad1.left_stick_x;
            double rx = gamepad1.right_stick_x;


            // This button choice was made so that it is hard to hit on accident,
            // it can be freely changed based on preference.
            // The equivalent button is start on Xbox-style controllers.
            if (gamepad1.y) imu.resetYaw();

            // Drop the fucking nigger shit spinner thingie >:(
            if (!isSpinArmDown) {
                leftSpinArm.setPower(1);
                sleep(2000);
                leftSpinArm.setPower(0);
                isSpinArmDown = true;
            }

            /* ##################################################
                        Movement Controls Calculations
               ################################################## */

            double rotX = x * Math.cos(botHeading) - y * Math.sin(-botHeading);
            double rotY = x * Math.sin(-botHeading) + y * Math.cos(botHeading);

            rotX *= PARAMS.speedMult;
            rotY *= PARAMS.speedMult;
            rx   *= PARAMS.turnMult;

            /* ##################################################
                                   Rotation
               ################################################## */

            if (rx == 0 && isTurning) {
                wantedAngle = botHeading % (Math.PI*2);
                isTurning = false;
            }
            else if (rx != 0 && !isTurning) {
                isTurning = true;
            }
            double fixError = pid.calculate(botHeading);
            //makes sure the robot doesn't fix small angles
            if (Math.abs(Math.toDegrees(botHeading-wantedAngle)) > 6 && !isTurning){
                rx -= fixError;
            }

            pid.setSetPoint(wantedAngle);

            /* ##################################################
                                  Front Arm
               ################################################## */

            if (gamepad1.right_trigger > 0) {
                frontArm.setPower(1);
            }
            else if ( gamepad1.left_trigger > 0) {
                frontArm.setPower(-1);
            }
            else frontArm.setPower(0);

            /*###################################################
                                Spinner
              ################################################### */

            if (gamepad1.a && isSpinnerSpinning) {
                spinner.setPower(0);
                isSpinnerSpinning = false;
            }
            else if (gamepad1.a && !isSpinnerSpinning) {
                spinner.setPower(1);
                isSpinnerSpinning = true;
            }

            if (gamepad2.right_bumper) {
                leftDrive.setPower(1);
                rightDrive.setPower(1);

                leftDrive.setTargetPosition(3500);
                rightDrive.setTargetPosition(3500);
            } else if (gamepad2.left_bumper) {
                leftDrive.setPower(1);
                rightDrive.setPower(1);
                leftDrive.setTargetPosition(250);
                rightDrive.setTargetPosition(250);
            }

            while (gamepad2.y) {
                leftSlide.setPower(1);
                rightSlide.setPower(-1);
            }
            while (gamepad2.a) {
                leftSlide.setPower(-1);
                rightSlide.setPower(1);
            }
            leftSlide.setPower(0);
            rightSlide.setPower(0);

            /// THIS IS NEW STUFF FOR CLAW AND ROT ///
            /// CHANGE BINDINGS ACCORDING TO DRIVERS

            // CLAW SERVO
            if (gamepad2.b) {
                clawServo.setPosition(0.48); // open
            } else if(gamepad2.x) {
                clawServo.setPosition(0.35); // close
            }

//            // ROTATOR SERVO - DISABLED
//            if (gamepad1.y) {
//                rotator.setPosition(0.9); // up?
//            } else if(gamepad1.a) {
//                rotator.setPosition(0.25); // should be down
//            }

            if (gamepad1.b) {
                upReleaseAndReset(clawServo, leftElevator, rightElevator, rotator);
            }
            if (gamepad1.y) {
                grabCubeAndUp(clawServo, leftElevator, rightElevator, rotator);
            }

            /* BINDINGS SO FAR - SHOW DRIVERS
            - frontarm forwards/backwards: right/left triggers - gamepad 1
            - spinner action: a - gamepad 1
            - rotatorarm up/down: y/a - gamepad 1
            - slide up/down: y/a - gamepad 2
            - claw open/close: b/x gamepad 2
            - elevator up/down: right/left bumpers: gamepad 2
             */

             /* ######################################################
                   Runs Autonomous Actions in TeleOp - TODO: Enable
                ###################################################### */

//           // update running actions
//           List<Action> newActions = new ArrayList<>();
//            for (Action action : runningActions) {
//                action.preview(packet.fieldOverlay());
//                if (action.run(packet)) {
//                    newActions.add(action);
//                }
//           }
//            runningActions = newActions;
//
//           dash.sendTelemetryPacket(packet);

            /* ##################################################
                     Applying the Calculations to the Motors
               ################################################## */

            // Denominator is the largest motor power (absolute value) or 1
            // This ensures all the powers maintain the same ratio,
            // but only if at least one is out of the range [-1, 1]
            double denominator     = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(rx), 1);
            double frontLeftPower  = PARAMS.frontMotorMult *PARAMS.leftMotorMult* (rotY + rotX + rx) / denominator;
            double backLeftPower   = PARAMS.backMotorMult *PARAMS.leftMotorMult* (rotY - rotX + rx) / denominator;
            double frontRightPower = PARAMS.frontMotorMult * PARAMS.rightMotorMult * (rotY - rotX - rx) / denominator;
            double backRightPower  = PARAMS.backMotorMult *PARAMS.rightMotorMult* (rotY + rotX - rx) / denominator;

            frontLeftMotor.setPower(frontLeftPower);
            backLeftMotor.setPower(backLeftPower);
            frontRightMotor.setPower(frontRightPower);
            backRightMotor.setPower(backRightPower);


            /* ##################################################
                             TELEMETRY ADDITIONS
               ################################################## */

            telemetry.addData("Angle (Degrees)", Math.toDegrees(botHeading));
            telemetry.addData("rx:", rx);
            telemetry.addData("Wanted Angle", wantedAngle);
            telemetry.addData("Bot Heading", botHeading);

            // Arm Encoders
            telemetry.addData("Arm Right Position", rightDrive.getCurrentPosition());
            telemetry.addData("Arm Left Position", leftDrive.getCurrentPosition());
            telemetry.addData("Front Arm Position", frontArm.getCurrentPosition());

            // CRServo Debugging
            telemetry.addData("CRServo Power (leftSpinArm)", leftSpinArm.getPower());

            // Spinner Motor
            telemetry.addData("Spinner Power", spinner.getPower());

            // Front Arm Motor
            telemetry.addData("Front Arm Motor Power", frontArm.getPower());

            // Servo Positions
            telemetry.addData("Claw Servo Position", clawServo.getPosition());
            telemetry.addData("Rotator Servo Position", rotator.getPosition());


            telemetry.update();

        }
    }

    private void grabCubeAndUp(Servo claw, DcMotor leftel, DcMotor rightel, Servo rot) {
        leftel.setTargetPosition(250);
        rightel.setTargetPosition(250);
        rot.setPosition(0.25);
        sleep(500);
        claw.setPosition(0.35); // catch
        sleep(250);
        rightel.setTargetPosition(3500);
        leftel.setTargetPosition(3500);
    }

    private void upReleaseAndReset(Servo claw, DcMotor leftel, DcMotor rightel, Servo rot) {
        leftel.setTargetPosition(3500);
        rightel.setTargetPosition(3500);
        sleep(250);
        rot.setPosition(1);
        claw.setPosition(0.5);
        sleep(250);
        rot.setPosition(0.25);
        rightel.setTargetPosition(250);
        leftel.setTargetPosition(250);
    }

    private double unwrapAngle(double previousAngle, double currentAngle) {
        double delta = currentAngle - previousAngle;
        if (delta > Math.PI) {
            delta -= 2 * Math.PI;
        } else if (delta < -Math.PI) {
            delta += 2 * Math.PI;
        }
        return previousAngle + delta;
    }
}