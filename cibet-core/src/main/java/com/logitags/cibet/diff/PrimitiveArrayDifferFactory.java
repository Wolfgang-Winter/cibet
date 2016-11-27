package com.logitags.cibet.diff;

import de.danielbechler.diff.NodeQueryService;
import de.danielbechler.diff.differ.Differ;
import de.danielbechler.diff.differ.DifferDispatcher;
import de.danielbechler.diff.differ.DifferFactory;

public class PrimitiveArrayDifferFactory implements DifferFactory {

   public Differ createDiffer(DifferDispatcher differDispatcher, NodeQueryService nodeQueryService) {
      return new PrimitiveArrayDiffer();
   }

}
