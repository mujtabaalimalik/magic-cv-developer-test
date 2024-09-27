// Copyright (c) 2024 Magic Tech Ltd

package fit.magic.cv.repcounter

import fit.magic.cv.PoseLandmarkerHelper
import kotlin.math.*
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class ExerciseRepCounterImpl : ExerciseRepCounter() {
    // Some private variables
    private val lungeThreshold = 0.9f
    private val standThreshold = 0.1f
    private var objective = ""

    override fun setResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        // process pose data in resultBundle
        if (resultBundle.results[0].landmarks().isNotEmpty()) {
            val lunge = detectLunge(resultBundle.results[0].landmarks())
            val progress = calculateProgress(resultBundle.results[0].landmarks())
            if (progress < standThreshold) {
                sendProgressUpdate(0f)
                if (objective == "SR") {
                    objective = "R"
                } else if (objective == "SL"){
                    objective = "L"
                }
                if (objective == "R") {
                    sendFeedbackMessage("Do a right lunge")
                } else if (objective == "L") {
                    sendFeedbackMessage("Do a left lunge")
                } else {
                    sendFeedbackMessage("Do a lunge")
                }
            } else if (progress > standThreshold && progress < lungeThreshold) {
                if (objective == "SR" || objective == "SL") {
                    sendProgressUpdate(progress)
                    sendFeedbackMessage("Return to standing position")
                } else {
                    if (objective == lunge || objective == ""){
                        sendProgressUpdate(progress)
                        sendFeedbackMessage("Continue with your rep")
                    } else {
                        sendProgressUpdate(0f)
                        sendFeedbackMessage("Do an alternate lunge. Switch legs.")
                    }
                }
            } else if (progress > lungeThreshold) {
                if (objective == lunge || objective == "") {
                    sendProgressUpdate(progress)
                    incrementRepCount()
                    if (objective == "L" || (objective == "" && lunge == "L")) {
                        objective = "SR"
                        sendFeedbackMessage("Left lunge complete, return to standing position")
                    } else if (objective == "R" || (objective == "" && lunge == "R")) {
                        objective = "SL"
                        sendFeedbackMessage("Right lunge complete, return to standing position")
                    }
                } else {
                    sendProgressUpdate(0f)
                    sendFeedbackMessage("Do an alternate lunge. Switch legs.")
                }
            }
        }
//        resetRepCount()

        // use functions in base class incrementRepCount(), sendProgressUpdate(),
        // and sendFeedbackMessage() to update the UI
    }

    private fun detectLunge(landmarks: List<List<NormalizedLandmark>>): String {
        var lungeDirection = ""
        val poseLandmarks = landmarks[0]
        if (poseLandmarks.size > 28) {
            // Extract the landmarks by their respective indexes
            val leftHip = poseLandmarks[23]
            printLandmark("Left Hip", leftHip)
            val rightHip = poseLandmarks[24]
            printLandmark("Right Hip", rightHip)
            val leftKnee = poseLandmarks[25]
            printLandmark("Left Knee", leftKnee)
            val rightKnee = poseLandmarks[26]
            printLandmark("Right Knee", rightKnee)
            val leftAnkle = poseLandmarks[27]
            printLandmark("Left Ankle", leftAnkle)
            val rightAnkle = poseLandmarks[28]
            printLandmark("Right Ankle", rightAnkle)
            val leftHeel = poseLandmarks[29]
            printLandmark("Left Heel", leftHeel)
            val rightHeel = poseLandmarks[30]
            printLandmark("Right Heel", rightHeel)
            val leftToe = poseLandmarks[31]
            printLandmark("Left Toe", leftToe)
            val rightToe = poseLandmarks[32]
            printLandmark("Right Toe", rightToe)

            val direction = detectSubjectDirection(leftToe, rightToe, leftHeel, rightHeel)
            lungeDirection = detectLungeDirection(direction, leftKnee, rightKnee, leftHip, rightHip, leftAnkle, rightAnkle)
        } else {
            println("Insufficient landmarks detected for the pose.")
        }
    return(lungeDirection)
    }

    private fun detectSubjectDirection(
        leftToes: NormalizedLandmark, rightToes: NormalizedLandmark,
        leftHeel: NormalizedLandmark, rightHeel: NormalizedLandmark
    ): String {
        // Calculate average x and z positions of both feet
        val leftFootX = (leftToes.x() + leftHeel.x()) / 2
        val rightFootX = (rightToes.x() + rightHeel.x()) / 2
        val leftFootZ = (leftToes.z() + leftHeel.z()) / 2
        val rightFootZ = (rightToes.z() + rightHeel.z()) / 2

        // Check which foot is more forward (in terms of z-coordinates) for depth
        return if (rightFootX > leftFootX && rightFootZ < leftFootZ) {
            "F" // forward or right
        } else {
            "B" // backward or left
        }
    }

    private fun detectLungeDirection(
        facingDirection: String,
        leftKnee: NormalizedLandmark, rightKnee: NormalizedLandmark,
        leftHip: NormalizedLandmark, rightHip: NormalizedLandmark,
        leftAnkle: NormalizedLandmark, rightAnkle: NormalizedLandmark
    ): String {
        // Compare knee z-coordinates to determine which leg is moving forward in depth
        val leftKneeZ = leftKnee.z()
        val rightKneeZ = rightKnee.z()
        // Compare knee y-coordinates to determine which leg is bending downwards (closer to the ground)
        val leftKneeY = leftKnee.y()
        val rightKneeY = rightKnee.y()
        // Check which leg is moving down and forward based on the facing direction
        return when (facingDirection) {
            "F" -> {
                if (leftKneeY > rightKneeY && leftKneeZ > rightKneeZ) {
                    "R"
                } else if (rightKneeY > leftKneeY && rightKneeZ > leftKneeZ) {
                    "L"
                } else {
                    ""
                }
            }
            "B" -> {
                if (rightKneeY > leftKneeY && rightKneeZ < leftKneeZ) {
                    "R"
                } else if (leftKneeY > rightKneeY && leftKneeZ < rightKneeZ) {
                    "L"
                } else {
                    ""
                }
            }
            else -> {""}
        }
    }

    private fun calculateProgress(landmarks: List<List<NormalizedLandmark>>): Float {
        var progress = 0f
        if (landmarks.isNotEmpty()) {
            val poseLandmarks = landmarks[0]
            if (poseLandmarks.size > 28) {
                val leftHip = poseLandmarks[23]
                printLandmark("Left Hip", leftHip)
                val rightHip = poseLandmarks[24]
                printLandmark("Right Hip", rightHip)
                val leftKnee = poseLandmarks[25]
                printLandmark("Left Knee", leftKnee)
                val rightKnee = poseLandmarks[26]
                printLandmark("Right Knee", rightKnee)
                val leftAnkle = poseLandmarks[27]
                printLandmark("Left Ankle", leftAnkle)
                val rightAnkle = poseLandmarks[28]
                printLandmark("Right Ankle", rightAnkle)

                val leftLegAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
                val rightLegAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
                println("Left leg angle: $leftLegAngle, Right leg angle: $rightLegAngle")

                var leftLegProgress = (180 - leftLegAngle) / 90
                if (leftLegProgress > 1) {
                    leftLegProgress = 1.0f
                }
                println("Left leg progress: $leftLegProgress")
                var rightLegProgress = (180 - leftLegAngle) / 90
                if (rightLegProgress > 1) {
                    rightLegProgress = 1.0f
                }
                println("Right leg progress: $rightLegProgress")

                progress = ((0.5*leftLegProgress) + (0.5*rightLegProgress)).toFloat()
                println("Total progress: $progress")
            } else {
                println("Insufficient landmarks detected for the pose.")
            }
        } else {
            println("No poses detected.")
        }
        return progress
    }

    // Function to print the landmark data
    private fun printLandmark(name: String, landmark: NormalizedLandmark) {
        val x = landmark.x()  // Normalized x-coordinate [0, 1] relative to image width
        val y = landmark.y()  // Normalized y-coordinate [0, 1] relative to image height
        val z = landmark.z()  // Normalized z-coordinate (depth)
        val visibility = landmark.visibility()  // The visibility score of the landmark (optional)
        println("$name - X: $x, Y: $y, Z: $z, Visibility: $visibility")
    }

    private fun calculateAngle(hip: NormalizedLandmark, knee: NormalizedLandmark, ankle: NormalizedLandmark): Float {
        val hipPoint = Point3D(hip.x(), hip.y(), hip.z())
        val kneePoint = Point3D(knee.x(), knee.y(), knee.z())
        val anklePoint = Point3D(ankle.x(), ankle.y(), ankle.z())

        val angle = calculateAngleBetweenLines(hipPoint, kneePoint, anklePoint)
        return(angle)
    }

    // Data class to represent a 3D point
    data class Point3D(val x: Float, val y: Float, val z: Float)

    // Function to calculate the vector between two points
    private fun vectorBetweenPoints(p1: Point3D, p2: Point3D): Point3D {
        return Point3D(p1.x - p2.x, p1.y - p2.y, p1.z - p2.z)
    }

    // Function to calculate the dot product of two vectors
    private fun dotProduct(v1: Point3D, v2: Point3D): Float {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
    }

    // Function to calculate the magnitude of a vector
    private fun magnitude(v: Point3D): Float {
        return sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
    }

    // Function to calculate the angle between two lines formed by hip-knee and ankle-knee
    private fun calculateAngleBetweenLines(hip: Point3D, knee: Point3D, ankle: Point3D): Float {
        // Vector from hip to knee
        val v1 = vectorBetweenPoints(hip, knee)

        // Vector from ankle to knee
        val v2 = vectorBetweenPoints(ankle, knee)

        // Dot product of v1 and v2
        val dotProd = dotProduct(v1, v2)

        // Magnitude of v1 and v2
        val magV1 = magnitude(v1)
        val magV2 = magnitude(v2)

        // Calculate the angle in radians using the dot product formula
        val cosTheta = dotProd / (magV1 * magV2)

        // Convert the angle from radians to degrees
        return acos(cosTheta) * (180.0f / PI.toFloat())
    }
}
