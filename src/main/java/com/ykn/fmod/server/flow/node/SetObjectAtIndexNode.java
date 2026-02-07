/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.FlowNode;

/**
 * A node to set an object at a specific index in a list
 * Inputs:
 * 1. List - The list to set the object in, can be any type of list, if not a list, it will be treated as a single element list containing the object, if null, it will be treated as an empty list
 * 2. Number - The index of the object to set, if negative, it will count from the end of the list, if out of bounds, the list will be extended with null values until the index is valid, if null, it will be treated as list size (append to the end of the list)
 * 3. Object - The object to set at the specified index in the list, can be null
 * 4. Boolean - If true, replace the existing element at the specified index, if false, insert the object at the specified index and shift the existing elements to the right. Treat null value as true.
 * 5. Boolean - If true and the Object input is also a list, (if isReplace is true, add all its elements to the target list starting from the specified index. If isReplace is false, overwrite elements starting from the specified index), otherwise treat the Object as a single element at the specified index. Treat null value as true.
 * 6. Boolean - If true, strip all the null values at the beginning and the end of the list after setting the object, otherwise keep the null values. Treat null value as (false if [index is negative AND out of bounds which make the list extended with null values to satisfy the index] else true).
 * Outputs:
 * 1. List - The list after setting the object
 * 2. Object - The original object at the specified index in the list before setting, if isReplace is true and isFlatten is true, this is a list with all affected original elements, otherwise this is a single element, if the index is out of bounds, this will be null
 * 3. Number - The number of elements in the list before setting the object
 * 4. Number - The number of elements in the list after setting the object
 * Branches: 1 (Next node)
 */
public class SetObjectAtIndexNode extends FlowNode {
    
}
