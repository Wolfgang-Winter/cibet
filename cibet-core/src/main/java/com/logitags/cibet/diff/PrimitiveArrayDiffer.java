package com.logitags.cibet.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.danielbechler.diff.access.Instances;
import de.danielbechler.diff.comparison.ComparisonStrategy;
import de.danielbechler.diff.differ.Differ;
import de.danielbechler.diff.node.DiffNode;

/**
 * Differ that allows comparing arrays with primitive elements in an all-or-nothing different strategy. No nodes on the
 * array element level are created.
 * 
 */
public class PrimitiveArrayDiffer implements Differ {

   private static Log log = LogFactory.getLog(PrimitiveArrayDiffer.class);

   public boolean accepts(Class<?> type) {
      return type != null && type.isArray() && type.getComponentType().isPrimitive();
   }

   public DiffNode compare(DiffNode parentNode, Instances instances) {
      final DiffNode beanNode = new DiffNode(parentNode, instances.getSourceAccessor(), instances.getType());
      log.debug("PrimitiveArrayDiffer compare node " + beanNode);

      if (instances.areNull() || instances.areSame()) {
         beanNode.setState(DiffNode.State.UNTOUCHED);
      } else if (instances.hasBeenAdded()) {
         beanNode.setState(DiffNode.State.ADDED);
      } else if (instances.hasBeenRemoved()) {
         beanNode.setState(DiffNode.State.REMOVED);
      } else {
         ComparisonStrategy strategy = new ArrayComparisonStrategy();
         strategy.compare(beanNode, instances.getType(), instances.getWorking(), instances.getBase());
      }
      return beanNode;
   }

}
