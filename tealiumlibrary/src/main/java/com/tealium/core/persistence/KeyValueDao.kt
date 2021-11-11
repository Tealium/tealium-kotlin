package com.tealium.core.persistence

/**
 * @param K - Identifier Type
 * @param T - Type to be accessed
 */
interface KeyValueDao<K, T> {

    /**
     * Fetch and item given its [key]
     */
    fun get(key: K): T?

    /**
     * Fetch all items in the storage. Returning as a map of key/value pairs.
     */
    fun getAll(): Map<K, T>

    /**
     * Attempts to save an item in the storage, should not check if an item exists already with the
     * same key - see [upsert]
     */
    fun insert(item: T)

    /**
     * Attempts to update an existing entry in the storage, should not check if an item with the
     * given key already exists - see [upsert]
     */
    fun update(item: T)

    /**
     * Removes and item from storage given the [key].
     */
    fun delete(key: K)

    /**
     * Should check whether an item exists at the given key before choosing to [insert] or [update]
     * accordingly.
     */
    fun upsert(item: T)

    /**
     * Removes all entries from the storage.
     */
    fun clear()

    /**
     * Returns a list of keys identifying the current set of items stored.
     */
    fun keys(): List<K>

    /**
     * Returns the number of items currently stored.
     */
    fun count(): Int

    /**
     * Returns true if an item with the given [key] is currently stored, else returns false.
     */
    fun contains(key: K): Boolean

    /**
     * Removes all expired entries from storage.
     */
    fun purgeExpired()

}