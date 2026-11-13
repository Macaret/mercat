package com.smartcodeapps.constructionprizewheel.domain

data class RearrangeResult(val draft: PrizeDraft, val pushedOut: List<Prize>)

/** An editable copy of the prizes. New prizes get negative temporary ids until they are saved. */
class PrizeDraft private constructor(val prizes: List<Prize>, private val nextTempId: Long) {

    constructor(prizes: List<Prize>) : this(prizes, -1L)

    val activeCount: Int get() = prizes.count { it.active }

    val canActivateMore: Boolean get() = activeCount < MAX_ACTIVE_PRIZES

    fun add(name: String, description: String?, imagePath: String?, probability: Int, active: Boolean): PrizeDraft {
        require(!active || canActivateMore) { "Cannot activate more than $MAX_ACTIVE_PRIZES prizes" }
        val prize = Prize(
            id = nextTempId,
            name = name,
            description = description,
            imagePath = imagePath,
            probability = probability,
            active = active,
            wheelOrder = if (active) nextWheelOrder() else null
        )
        return PrizeDraft(prizes + prize, nextTempId - 1)
    }

    fun update(
        id: Long,
        name: String,
        description: String?,
        imagePath: String?,
        probability: Int,
        active: Boolean
    ): PrizeDraft {
        val existing = prizes.first { it.id == id }
        val activating = active && !existing.active
        require(!activating || canActivateMore) { "Cannot activate more than $MAX_ACTIVE_PRIZES prizes" }
        val wheelOrder = when {
            activating -> nextWheelOrder()
            !active -> null
            else -> existing.wheelOrder
        }
        val updated = existing.copy(
            name = name,
            description = description,
            imagePath = imagePath,
            probability = probability,
            active = active,
            wheelOrder = wheelOrder
        )
        return PrizeDraft(prizes.map { if (it.id == id) updated else it }, nextTempId)
    }

    fun delete(id: Long): PrizeDraft = PrizeDraft(prizes.filterNot { it.id == id }, nextTempId)

    /**
     * Makes [activeIds] (in this order) the active prizes and every other prize inactive.
     * If more than [MAX_ACTIVE_PRIZES] are requested, the prize that was last on the wheel is pushed out.
     */
    fun rearrange(activeIds: List<Long>): RearrangeResult {
        val byId = prizes.associateBy { it.id }
        val previousIds = prizes.wheelPrizes().map { it.id }
        var newActive = activeIds.filter { it in byId }.distinct()

        val pushedOut = mutableListOf<Prize>()
        while (newActive.size > MAX_ACTIVE_PRIZES) {
            val pushedId = previousIds.lastOrNull { it in newActive } ?: newActive.last()
            pushedOut += byId.getValue(pushedId)
            newActive = newActive - pushedId
        }
        if (newActive == previousIds) return RearrangeResult(this, pushedOut)

        val order = newActive.withIndex().associate { (index, id) -> id to index + 1 }
        val updated = prizes.map { prize ->
            val position = order[prize.id]
            if (position != null) prize.copy(active = true, wheelOrder = position)
            else prize.copy(active = false, wheelOrder = null)
        }
        return RearrangeResult(PrizeDraft(updated, nextTempId), pushedOut)
    }

    private fun nextWheelOrder(): Int =
        (prizes.filter { it.active }.mapNotNull { it.wheelOrder }.maxOrNull() ?: 0) + 1
}

fun List<Prize>.forAdminList(): List<Prize> =
    wheelPrizes() + filterNot { it.active }.sortedBy { it.name.lowercase() }
