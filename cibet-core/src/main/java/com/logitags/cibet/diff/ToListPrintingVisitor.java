/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.DiffNode.State;
import de.danielbechler.diff.node.PrintingVisitor;
import de.danielbechler.diff.node.Visit;
import de.danielbechler.diff.path.NodePath;
import de.danielbechler.diff.selector.CollectionItemElementSelector;
import de.danielbechler.diff.selector.ElementSelector;
import de.danielbechler.diff.selector.MapKeyElementSelector;
import de.danielbechler.diff.selector.RootElementSelector;

public class ToListPrintingVisitor extends PrintingVisitor {

   private transient Log log = LogFactory.getLog(ToListPrintingVisitor.class);

   private List<Difference> differences = new ArrayList<Difference>();

   private final Object working;
   private final Object base;

   public ToListPrintingVisitor(Object working, Object base) {
      super(working, base);
      this.base = base;
      this.working = working;
   }

   public void node(final DiffNode node, final Visit visit) {
      if (filter(node)) {
         final String text = differenceToString(node, base, working);
         log.info(text);
      }
   }

   protected boolean filter(final DiffNode node) {
      boolean skip = node.isRootNode()
            || (node.getState() == State.CHANGED && node.hasChildren())
            || (node.getParentNode() != null && (node.getParentNode().getState() == State.ADDED || node.getParentNode()
                  .getState() == State.REMOVED));
      return !skip;
   }

   protected String differenceToString(final DiffNode node, final Object base, final Object modified) {
      final String text = super.differenceToString(node, base, modified);

      Difference difference = new Difference();
      switch (node.getState()) {
      case ADDED:
         difference.setDifferenceType(DifferenceType.ADDED);
         break;
      case CHANGED:
         difference.setDifferenceType(DifferenceType.MODIFIED);
         break;
      case REMOVED:
         difference.setDifferenceType(DifferenceType.REMOVED);
         break;
      default:
         difference.setDifferenceType(DifferenceType.NOT_SPECIFIED);
         return text;
      }

      difference.setPropertyPath(node.getPath().toString());
      difference.setCanonicalPath(canonicalPath(node.getPath()));
      difference.setOldValue(node.canonicalGet(base));
      difference.setNewValue(node.canonicalGet(modified));
      difference.setPropertyName(node.getPropertyName());
      difference.setPropertyType(node.getValueType());
      log.debug(difference);

      differences.add(difference);

      return text;
   }

   private String canonicalPath(final NodePath node) {
      StringBuilder sb = new StringBuilder();
      Iterator<ElementSelector> iterator = node.getElementSelectors().iterator();
      ElementSelector elementSelector;
      for (ElementSelector previousElementSelector = null; iterator.hasNext(); previousElementSelector = elementSelector) {
         elementSelector = iterator.next();
         if (elementSelector instanceof RootElementSelector) {

         } else if ((elementSelector instanceof CollectionItemElementSelector)
               || (elementSelector instanceof MapKeyElementSelector)) {

         } else if (previousElementSelector instanceof RootElementSelector) {
            sb.append(elementSelector);
         } else {
            sb.append('.');
            sb.append(elementSelector);
         }
      }

      return sb.toString();
   }

   /**
    * @return the differences
    */
   public List<Difference> getDifferences() {
      return differences;
   }

}
