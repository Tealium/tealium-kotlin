package com.tealium.core.persistence

interface QueueingDao<K, T> : KeyValueDao<K, T> {

    /**
     * Should add an [item] of type [T] to the queue.
     */
    fun enqueue(item: T)

    /**
     * Should add each entry of a list of [items] of type [T] to the queue, maintaining the order from
     * the List.
     */
    fun enqueue(items: List<T>)

    /**
     * Should retrieve the oldest entry from the queue.
     */
    fun dequeue(): T?

    /**
     * Should retrieve the oldest [count] entries from the queue.
     */
    fun dequeue(count: Int): List<T>

    /**
     * Should resize the queue, removing entries from the queue if necessary.
     */
    fun resize(size: Int)
}