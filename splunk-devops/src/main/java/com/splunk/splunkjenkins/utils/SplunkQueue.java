package com.splunk.splunkjenkins.utils;

import hudson.ExtensionPoint;

import com.splunk.splunkjenkins.model.EventRecord;

public interface SplunkQueue extends ExtensionPoint {
    /**
     * Removes all of the elements from this queue.
     * The queue will be empty after this method returns.
     *
     * @throws UnsupportedOperationException if the clear operation
     *         is not supported by this collection
     */
    void clear();

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available; will also try to clear the queue,
     * if congestion is suspected.
     *
     * @param record the element to add
     * @return true if the element was added to this queue, else
     *         false
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean enqueue(EventRecord record);

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     *
     * @param record the element to add
     * @return true if the element was added to this queue, else
     *         false
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    boolean offer(EventRecord record);

    /**
     * Returns the number of elements in this collection.
     *
     * @return the number of elements in this collection
     */
    int size();

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    EventRecord take() throws InterruptedException;
}
