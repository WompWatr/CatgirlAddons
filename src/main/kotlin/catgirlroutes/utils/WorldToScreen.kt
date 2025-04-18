package catgirlroutes.utils

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.*

object WorldToScreen {
    fun getMatrix(matrix: Int): Matrix4f {
        val floatBuffer = BufferUtils.createFloatBuffer(16)
        GL11.glGetFloat(matrix, floatBuffer)
        return Matrix4f().load(floatBuffer) as Matrix4f
    }

    fun worldToScreen(pointInWorld: Vector3f, screenWidth: Int, screenHeight: Int): Vector2f? {
        return worldToScreen(pointInWorld, getMatrix(GL11.GL_MODELVIEW_MATRIX), getMatrix(GL11.GL_PROJECTION_MATRIX), screenWidth, screenHeight)
    }

    fun worldToScreen(
        pointInWorld: Vector3f,
        view: Matrix4f,
        projection: Matrix4f,
        screenWidth: Int,
        screenHeight: Int
    ): Vector2f? {
        val clipSpacePos = multiply(
            multiply(Vector4f(pointInWorld.x, pointInWorld.y, pointInWorld.z, 1.0f), view),
            projection
        )
        val ndcSpacePos = Vector3f(
            clipSpacePos.x / clipSpacePos.w,
            clipSpacePos.y / clipSpacePos.w,
            clipSpacePos.z / clipSpacePos.w
        )
        val screenX = (ndcSpacePos.x + 1.0f) / 2.0f * screenWidth
        val screenY = (1.0f - ndcSpacePos.y) / 2.0f * screenHeight
        if (ndcSpacePos.z < -1.0f || ndcSpacePos.z > 1.0f) {
            return null
        }
        return Vector2f(screenX, screenY)
    }

    fun multiply(vec: Vector4f, mat: Matrix4f): Vector4f {
        return Vector4f(
            vec.x * mat.m00 + vec.y * mat.m10 + vec.z * mat.m20 + vec.w * mat.m30,
            vec.x * mat.m01 + vec.y * mat.m11 + vec.z * mat.m21 + vec.w * mat.m31,
            vec.x * mat.m02 + vec.y * mat.m12 + vec.z * mat.m22 + vec.w * mat.m32,
            vec.x * mat.m03 + vec.y * mat.m13 + vec.z * mat.m23 + vec.w * mat.m33
        )
    }
}