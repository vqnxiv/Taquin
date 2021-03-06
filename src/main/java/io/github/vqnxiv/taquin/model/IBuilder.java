package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.controller.BuilderController;
import io.github.vqnxiv.taquin.model.Search;
import io.github.vqnxiv.taquin.util.Utils;
import javafx.beans.property.Property;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * Utility interface to simplify the use of builder classes.
 * <p>
 * Builders which are to be used in {@link BuilderController} should implement this interface.
 * 
 * @see Search.Builder
 * @see io.github.vqnxiv.taquin.model.SearchSpace.Builder
 * @see io.github.vqnxiv.taquin.model.DataStructure.Builder
 */
public interface IBuilder {

    /**
     * Enum which is used to classify the different kinds of {@link Property} which
     * can be returned from {@link #getBatchProperties()}.
     */
    enum Category {
        /**
         * Options for {@link io.github.vqnxiv.taquin.model.DataStructure.Builder} creation.
         */
        COLLECTION,
        /**
         * Main options for {@link Search} creation.
         * This is mainly for the properties in {@link Search.Builder}.
         */
        SEARCH_MAIN,
        /**
         * Extra options for {@link Search} creation.
         * This is mainly for the properties in the builder class of 
         * {@link Search} subclasses.
         */
        SEARCH_EXTRA,
        /**
         * Options for {@link Search.SearchLimit}.
         */
        LIMITS,
        /**
         * Options that are used for Search task creation.
         */
        MISCELLANEOUS;

        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }
    
    /**
     * Properties that should be handled separately (i.e not dumped in the tab pane).
     * 
     * @return {@link Map} of {@link Property} where the keys are the properties name 
     * ({@link Property#getName()}) specified when created.
     */
    Map<String, Property<?>> getNamedProperties();

    /**
     * Properties that will be automatically placed in the tabpane.
     * 
     * @return Map of lists of properties where the key is the {@link Category} which the 
     * properties should go in.
     */
    EnumMap<Category, List<Property<?>>> getBatchProperties();
}
