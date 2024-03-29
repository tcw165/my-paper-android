// Copyright Feb 2018-present boyw165@gmail.com
//
// Original authors: Robert Sedgewick and Kevin Wayne
// Maintainer author: TAI CHUN WANG
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.view.collisionSimulation

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The `CollisionSystem` class represents a collection of particles
 * moving in the unit box, according to the laws of elastic collision.
 * This event-based simulation relies on a priority queue.
 *
 *
 * For additional documentation,
 * see [Section 6.1](https://algs4.cs.princeton.edu/61event) of
 * *Algorithms, 4th Edition* by Robert Sedgewick and Kevin Wayne.
 *
 * Initializes a system with the specified collection of particles.
 * The individual particles will be mutated during the simulation.
 *
 * @param particles the array of particles
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
class CollisionSystem {
    protected var mSimulationUpToMs: Double = 0.0

    // The priority queue for collision event.
    val collisionEventsSize: Int get() = mPQ.size
    protected val mPQ = PriorityQueue<Event>()
    // The array of particles
    protected val mParticles: MutableList<Particle> = mutableListOf()
    val particlesSize: Int get() = mParticles.size

    // Clock in seconds.
    protected var mClock: Double = 0.0

    // State.
    protected val mIsStarted = AtomicBoolean(false)

    fun isStarted(): Boolean {
        return mIsStarted.get()
    }

    fun start(particles: Array<Particle>) {
        mIsStarted.set(false)

        // Defensive copy
        mParticles.clear()
        mParticles += particles

        // Init clock.
        mClock = 0.0
        mSimulationUpToMs = TEN_MINUTES_MS.toDouble() / 1000.0

        // Rebuild the event queue.
        mPQ.clear()
        mParticles.forEachIndexed { _, particle ->
            predict(particle, mClock, mSimulationUpToMs)
        }

        // Flag started.
        mIsStarted.set(true)
    }

    fun stop() {
        mIsStarted.set(false)
    }

    /**
     * Simulates the system of particles for the specified amount of time.
     */
    fun simulate(dt: Double): List<Particle> {
        if (!mIsStarted.get()) return emptyList()

        if (dt < 0.0) {
            throw IllegalArgumentException("Given dt=%d is negative".format(dt))
        }

        var lastClock = mClock
        mClock += dt

        // The main event-driven simulation loop
        while (!mPQ.isEmpty()) {
            // Get impending event, discard if invalidated
            val event = mPQ.peek()

            // Lazily discard the invalid events.
            if (!event.isValid) {
                mPQ.remove()
                continue
            }

            if (event.time <= mClock) {
                mPQ.remove()

                // Handle the particle-particle collision.
                val a = event.a
                val b = event.b

                // Physical collision, so update positions
                val shift = event.time - lastClock
                if (shift <= 0.0) {
                    throw IllegalStateException("negative shift time=%.3f".format(shift))
                }
                for (particle in mParticles) {
                    particle.move(shift)
                }

                // Process event
                if (a != null && b != null) {
                    // particle-particle collision
                    a.bounceOff(b)
                } else a?.bounceOffVerticalWall() ?: b?.bounceOffHorizontalWall()

                // Update the priority queue with new collisions involving a or b
                predict(a, event.time, mSimulationUpToMs)
                predict(b, event.time, mSimulationUpToMs)

                // Advance the last clocking time.
                lastClock = event.time
            } else {
                // Update position.
                val shift = mClock - lastClock
                for (particle in mParticles) {
                    val lastX = particle.centerX
                    val lastY = particle.centerY

                    particle.move(shift)

                    if (particle.centerX <= 0.0 || particle.centerX >= 1.0 ||
                        particle.centerY <= 0.0 || particle.centerY >= 1.0) {
                        throw IllegalStateException(
                            "particle[vx=%.3f, vy=%.3f, dt=%.3f, shift=%.3f] runs off boundary: (x=%.3f, y=%.3f) => (x=%.3f, y=%.3f)"
                                .format(particle.vecX, particle.vecY,
                                        dt, shift,
                                        lastX, lastY,
                                        particle.centerX, particle.centerY))
                    }
                }
                break
            }
        }

        return mParticles
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    // updates priority queue with all new events for particle a
    private fun predict(thiz: Particle?,
                        currentMilliSeconds: Double,
                        upToMilliSeconds: Double) {
        if (thiz == null) return

        // particle-particle collisions
        for (that in mParticles) {
            val dt = thiz.timeToHit(that)
            if (dt == Double.POSITIVE_INFINITY || dt < 0.0) continue

            val t = currentMilliSeconds + dt
            // Overflow protection.
            if (t < 0.0) continue

            if (t <= upToMilliSeconds) {
                mPQ.add(Event(t, thiz, that))
            }
        }

        // particle-wall collisions
        val dtX = thiz.timeToHitVerticalWall()
        val dtY = thiz.timeToHitHorizontalWall()
        if (dtX >= 0.0 &&
            currentMilliSeconds + dtX > 0 &&
            currentMilliSeconds + dtX <= upToMilliSeconds) {
            mPQ.add(Event((currentMilliSeconds + dtX), thiz, null))
        }
        if (dtY >= 0.0 &&
            currentMilliSeconds + dtY > 0 &&
            currentMilliSeconds + dtY <= upToMilliSeconds) {
            mPQ.add(Event((currentMilliSeconds + dtY), null, thiz))
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    /***************************************************************************
     * An event during a particle collision simulation. Each event contains
     * the time at which it will occur (assuming no supervening actions)
     * and the particles a and b involved.
     *
     * -  a and b both null:      redraw event
     * -  a null, b not null:     collision with vertical wall
     * -  a not null, b null:     collision with horizontal wall
     * -  a and b both not null:  binary collision between a and b
     */
    protected class Event internal constructor(
        val time: Double,
        // time that event is scheduled to occur
        val a: Particle?,
        // particles involved in event, possibly null
        val b: Particle?) : Comparable<Event> {

        // collision counts at event creation
        private val countA: Int = a?.collisionCount ?: -1
        private val countB: Int = b?.collisionCount ?: -1

        // has any collision occurred between when event was created and now?
        internal val isValid: Boolean
            get() {
                if (a != null && a.collisionCount != countA) return false
                return if (b != null && b.collisionCount != countB) false else true
            }

        // compare times when two events will occur
        override fun compareTo(other: Event): Int {
            return when {
                this.time < other.time -> -1
                this.time > other.time -> 1
                else -> 0
            }
        }
    }

    companion object {
        private const val ONE_MINUTE_MS: Long = 1L * 60L * 1000L
        private const val TEN_MINUTES_MS: Long = 10L * 60L * 1000L
    }
}
