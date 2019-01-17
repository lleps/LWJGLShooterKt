class Box(
    var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0,
    var sx: Double = 0.0, var sy: Double = 0.0, var sz: Double = 0.0,
    var vx: Double = 0.0, var vy: Double = 0.0, var vz: Double = 0.0,
    var affectedByPhysics: Boolean = true,
    var color: Color = Color(),
    var inGround: Boolean = false
)

class Color(val r: Double = 1.0, val g: Double = 1.0, val b: Double = 1.0, val a: Double = 1.0) {
    companion object {
        val RED = Color(1.0, 0.0, 0.0)
        val GREEN = Color(0.0, 1.0, 0.0)
        val WHITE = Color(1.0, 1.0, 1.0)
    }
}