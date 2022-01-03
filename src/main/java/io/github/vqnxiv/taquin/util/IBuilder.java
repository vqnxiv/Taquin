package io.github.vqnxiv.taquin.util;


import io.github.vqnxiv.taquin.controller.BuilderController;
import javafx.beans.property.Property;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Utility interface to simplify the use of builder classes.
 * <p>
 * Builders which are to be used in {@link BuilderController} should implement this interface.
 * 
 * @see io.github.vqnxiv.taquin.solver.Search.Builder
 * @see io.github.vqnxiv.taquin.model.SearchSpace.Builder
 * @see io.github.vqnxiv.taquin.model.CollectionWrapper.Builder
 */
public interface IBuilder {
    /**
     * Properties that should be handled separately (i.e not dumped in the tab pane)
     * 
     * @return Map of Properties where the keys are the properties name specified when created
     */
    Map<String, Property<?>> getNamedProperties();

    /**
     * Properties that will be automatically placed in the tabpane
     * 
     * @return Map of lists of properties where the key is the name of the tab 
     * where the properties controls will be placed
     */
    EnumMap<BuilderController.TabPaneItem, List<Property<?>>> getBatchProperties();
}
