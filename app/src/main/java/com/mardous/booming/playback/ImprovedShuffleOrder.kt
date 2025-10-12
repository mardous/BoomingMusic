package com.mardous.booming.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * This is based on Gramophone's implementation, which is possibly the most complete to date.
 */
@UnstableApi
class ImprovedShuffleOrder private constructor(
    private val shuffled: IntArray,
    private val random: Random
) : ShuffleOrder {
    private val indexInShuffled = IntArray(shuffled.size)

    constructor(shuffled: IntArray, seed: Long) : this(shuffled.copyOf(), Random(seed))

    constructor(
        firstIndex: Int,
        length: Int,
        randomSeed: Long
    ) : this(firstIndex, length, Random(randomSeed))

    private constructor(
        firstIndex: Int,
        length: Int,
        random: Random
    ) : this(calculateListWithFirstIndex(calculateShuffledList(0, length, random), firstIndex), random)

    init {
        for (i in shuffled.indices) {
            indexInShuffled[shuffled[i]] = i
        }
    }

    override fun getLength(): Int {
        return shuffled.size
    }

    override fun getNextIndex(index: Int): Int {
        val shuffledIndex = indexInShuffled[index] + 1
        return if (shuffledIndex < shuffled.size) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        val shuffledIndex = indexInShuffled[index] - 1
        return if (shuffledIndex >= 0) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int {
        return if (shuffled.isNotEmpty()) shuffled[shuffled.size - 1] else C.INDEX_UNSET
    }

    override fun getFirstIndex(): Int {
        return if (shuffled.isNotEmpty()) shuffled[0] else C.INDEX_UNSET
    }

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        // TODO: Fix this scuffed hacky logic
        // TODO: Play next ordering needs to persist in unshuffle

        val newShuffled = IntArray(shuffled.size + insertionCount)
        val pivot: Int =
            if (insertionIndex < shuffled.size) {
                indexInShuffled[insertionIndex]
            } else {
                indexInShuffled.size
            }
        for (i in shuffled.indices) {
            var currentIndex = shuffled[i]
            if (currentIndex > insertionIndex) {
                currentIndex += insertionCount
            }

            if (i <= pivot) {
                newShuffled[i] = currentIndex
            } else {
                newShuffled[i + insertionCount] = currentIndex
            }
        }
        if (insertionIndex < shuffled.size) {
            for (i in 0 until insertionCount) {
                newShuffled[pivot + i + 1] = insertionIndex + i + 1
            }
        } else {
            for (i in 0 until insertionCount) {
                newShuffled[pivot + i] = insertionIndex + i
            }
        }
        return ImprovedShuffleOrder(newShuffled, Random(random.nextLong()))
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val numberOfElementsToRemove = indexToExclusive - indexFrom
        val newShuffled = IntArray(shuffled.size - numberOfElementsToRemove)
        var foundElementsCount = 0
        for (i in shuffled.indices) {
            if (shuffled[i] in indexFrom until indexToExclusive) {
                foundElementsCount++
            } else {
                newShuffled[i - foundElementsCount] =
                    if (shuffled[i] >= indexFrom) shuffled[i] - numberOfElementsToRemove
                    else shuffled[i]
            }
        }
        return ImprovedShuffleOrder(newShuffled, Random(random.nextLong()))
    }

    override fun cloneAndMove(indexFrom: Int, indexToExclusive: Int, newIndexFrom: Int): ShuffleOrder {
        return cloneAndRemove(indexFrom, indexToExclusive)
            .cloneAndInsert(newIndexFrom, indexToExclusive - indexFrom)
    }

    override fun cloneAndClear(): ShuffleOrder {
        return cloneAndRemove(0, shuffled.size)
    }

    @Serializable
    class SerializedOrder(
        @SerialName("data")
        val data: IntArray?,
        @SerialName("seed")
        val seed: Long
    ) {

        override fun toString(): String {
            return Json.encodeToString(this)
        }

        fun toShuffleOrder(
            firstIndex: Int = C.INDEX_UNSET,
            length: Int = C.LENGTH_UNSET
        ): ImprovedShuffleOrder {
            if (data == null) {
                if (firstIndex == C.INDEX_UNSET || length == C.INDEX_UNSET) {
                    throw IllegalArgumentException("Missing required firstIndex and/or length param")
                }
                return ImprovedShuffleOrder(firstIndex, length, Random(seed))
            } else {
                return ImprovedShuffleOrder(data, seed)
            }
        }

        companion object {
            fun serializedFromOrder(order: ImprovedShuffleOrder): SerializedOrder {
                return SerializedOrder(order.shuffled, order.random.nextLong())
            }

            fun serializedFromJson(content: String): SerializedOrder {
                return Json.decodeFromString<SerializedOrder>(content)
            }
        }
    }

    companion object {
        private fun calculateShuffledList(offset: Int, length: Int, random: Random): IntArray {
            val shuffled = IntArray(length)
            var swapIndex: Int
            for (i in shuffled.indices) {
                swapIndex = random.nextInt(i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = offset + i
            }
            return shuffled
        }

        private fun calculateListWithFirstIndex(shuffled: IntArray, firstIndex: Int): IntArray {
            if (shuffled.isEmpty() && firstIndex == 0) return shuffled
            if (shuffled.size <= firstIndex) throw IllegalArgumentException("${shuffled.size} <= $firstIndex")
            val fi = shuffled.indexOf(firstIndex)
            val before = shuffled.slice(0..<fi)
            val inclAndAfter = shuffled.slice(fi..<shuffled.size)
            return (inclAndAfter + before).toIntArray()
        }
    }
}
