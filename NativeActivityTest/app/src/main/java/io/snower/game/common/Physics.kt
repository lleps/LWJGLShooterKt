package io.snower.game.common

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import javax.vecmath.Matrix4f
import javax.vecmath.Vector3f

class Physics(
    private val mode: Mode,
    private val gravity: Vector3f = Vector3f(0f, -10f, 0f)
) : PhysicsInterface {

    // TODO: implement this class natively.

    enum class Mode {
        CLIENT, // follow the server (should predict?)
        SERVER  // generates the collisions
    }

    companion object {
        private const val DELTA_TO_BE_IN_GROUND = 0.01 // round error to go 0
    }

    private lateinit var world: DynamicsWorld
    private val boxes = mutableListOf<Box>()
    private var collisionCallback: (Box, Box) -> Unit = { _, _ -> }
    private val transform = Transform() // used for tmp

    override var lastSimulationMillis: Float = 0f
        private set

    fun init() {
        val broadphase = DbvtBroadphase()
        val collisionConfiguration = DefaultCollisionConfiguration()
        val collisionDispatcher = CollisionDispatcher(collisionConfiguration)
        val constraintSolver = SequentialImpulseConstraintSolver()
        world = DiscreteDynamicsWorld(collisionDispatcher, broadphase, constraintSolver, collisionConfiguration)
        world.setGravity(Vector3f(0f, -20f, 0f))
    }

    fun onCollision(callback: (Box, Box) -> Unit) {
        collisionCallback = callback
    }

    override fun register(box: Box) {
        if (box in boxes) return
        boxes += box
        val shape = if (box.isSphere) {
            SphereShape(box.size.x)
        } else {
            BoxShape(box.size.withOps { scale(0.5f) })
        }

        val inertia = Vector3f()
        if (box.affectedByPhysics) {
            shape.calculateLocalInertia(box.mass, inertia)
        } else {
            // Doesn't use inertia for players
        }
        val constructionInfo = if (box.affectedByPhysics) {
            RigidBodyConstructionInfo(
                box.mass,
                DefaultMotionState(Transform(
                    Matrix4f(box.rotation, box.position.get(), 1f)
                )),
                shape,
                inertia
            )
        } else {
            RigidBodyConstructionInfo(
                box.mass,
                DefaultMotionState(Transform(
                    Matrix4f(box.rotation, box.position.get(), 1f)
                )),
                shape,
                inertia
            )
        }
        //constructionInfo.restitution = box.bounceMultiplier
        val body = RigidBody(constructionInfo)
        if (box.isSphere || box.isCharacter) {
            body.ccdMotionThreshold = 0.2f
            body.activationState = CollisionObject.DISABLE_DEACTIVATION
        }
        box.rigidBody = body
        if (!box.affectedByPhysics) {
            body.friction = if (box.isCharacter) 0.01f else 0.95f
        }
        world.addRigidBody(body)
        body.userPointer = box
    }

    override fun unRegister(box: Box) {
        if (box !in boxes) return
        boxes -= box
        world.removeRigidBody(box.rigidBody!!)
        box.rigidBody?.destroy()
        box.rigidBody = null
    }

    override fun getBoxOpenGLMatrix(box: Box, dst: FloatArray) {
        box.rigidBody!!.getWorldTransform(transform)
        transform.getOpenGLMatrix(dst)
    }

    override fun simulate(delta: Int, updateObjs: Boolean, updateId: Int) {
        val start = System.nanoTime()
        world.stepSimulation(delta.toFloat() / 1000f)
        // now copy to the objects pos the real data
        val transform = Transform()
        for (box in boxes) {
            if (updateObjs || box.id == updateId) {
                box.syncing = true
                // copy to box the physics info
                val rigidBody = box.rigidBody!!
                rigidBody.motionState.getWorldTransform(transform)

                box.position.set(transform.origin)
                transform.getRotation(box.rotation)
                rigidBody.getLinearVelocity(box.linearVelocity)
                rigidBody.getAngularVelocity(box.angularVelocity)
                box.inGround = Math.abs(box.linearVelocity.y) < DELTA_TO_BE_IN_GROUND
                box.syncing = false
            }
        }

        val numManifolds = world.dispatcher.numManifolds
        for (i in 0 until numManifolds) {
            val contactManifold = world.dispatcher.getManifoldByIndexInternal(i) ?: continue
            val obA = contactManifold.body0 as RigidBody
            val obB = contactManifold.body1 as RigidBody

            val numContacts = contactManifold.numContacts
            for (j in 0 until numContacts) {
                val pt = contactManifold.getContactPoint(j)
                if (pt.distance < 0f) {
                    collisionCallback(obA.userPointer as Box, obB.userPointer as Box)
                }
            }
        }
        lastSimulationMillis = (System.nanoTime() - start) / 1000000f
    }
}