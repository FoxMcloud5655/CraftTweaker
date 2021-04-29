package com.blamejared.crafttweaker.impl.recipes;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.CraftTweakerRegistry;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker.api.item.IIngredient;
import com.blamejared.crafttweaker.api.managers.IRecipeManager;
import com.blamejared.crafttweaker.api.recipes.IRecipeHandler;
import com.blamejared.crafttweaker.api.recipes.IReplacementRule;
import com.blamejared.crafttweaker.impl.actions.recipes.ActionReplaceRecipe;
import com.blamejared.crafttweaker.impl.brackets.RecipeTypeBracketHandler;
import com.blamejared.crafttweaker.impl.recipes.replacement.FullIIngredientReplacementRule;
import com.blamejared.crafttweaker.impl.recipes.wrappers.WrapperRecipe;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.mojang.datafixers.util.Pair;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.registry.Registry;
import org.openzen.zencode.java.ZenCodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ZenRegister
@ZenCodeType.Name("crafttweaker.api.recipes.Replacer")
@Document("vanilla/api/recipes/Replacer")
public final class Replacer {
   
    private final Collection<IRecipeManager> targetedManagers;
    private final Collection<? extends IRecipe<?>> targetedRecipes;
    private final List<IReplacementRule> rules;
    
    private Replacer(final Collection<? extends IRecipe<?>> recipes, final Collection<IRecipeManager> managers) {
        this.targetedManagers = Collections.unmodifiableCollection(managers);
        this.targetedRecipes = Collections.unmodifiableCollection(recipes);
        this.rules = new ArrayList<>();
    }
    
    @ZenCodeType.Method
    public static Replacer forRecipes(final WrapperRecipe... recipes) {
        if (recipes.length <= 0) {
            throw new IllegalArgumentException("Unable to create a replacer without any targeted recipes");
        }
        return new Replacer(Arrays.stream(recipes).map(WrapperRecipe::getRecipe).collect(Collectors.toSet()), Collections.emptyList());
    }
    
    @ZenCodeType.Method
    public static Replacer forTypes(final IRecipeManager... managers) {
        if (managers.length <= 0) {
            throw new IllegalArgumentException("Unable to create a replacer without any targeted recipe types");
        }
        return new Replacer(Collections.emptyList(), new HashSet<>(Arrays.asList(managers)));
    }
    
    @ZenCodeType.Method
    public static Replacer forAllTypes() {
        return forAllExcluding();
    }
    
    @ZenCodeType.Method
    public static Replacer forAllExcluding(final IRecipeManager... managers) {
        final List<IRecipeManager> managerList = Arrays.asList(managers);
        return new Replacer(
                Collections.emptyList(),
                RecipeTypeBracketHandler.getManagerInstances().stream().filter(manager -> !managerList.contains(manager)).collect(Collectors.toSet())
        );
    }
    
    //@ZenCodeType.Method // TODO("Expose replacement rule?")
    public Replacer replace(final IReplacementRule rule) {
        if (rule == IReplacementRule.EMPTY) return this;
        this.rules.add(rule);
        return this;
    }
    
    @ZenCodeType.Method
    public Replacer replaceFully(final IIngredient from, final IIngredient to) {
        return this.replace(FullIIngredientReplacementRule.create(from, to));
    }
    
    @ZenCodeType.Method
    public void execute() {
        if (this.rules.isEmpty()) return;
        
        final List<IReplacementRule> rules = Collections.unmodifiableList(this.rules);

        Stream.concat(this.streamManagers(), this.streamRecipes())
                .map(pair -> this.execute(pair.getFirst(), pair.getSecond(), rules))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(ActionReplaceRecipe::apply);
    }
    
    private Stream<Pair<IRecipeManager, IRecipe<?>>> streamManagers() {
        return this.targetedManagers.stream()
                .map(manager -> Pair.of(manager, manager.getAllRecipes().stream().map(WrapperRecipe::getRecipe)))
                .flatMap(pair -> pair.getSecond().map(recipe -> Pair.of(pair.getFirst(), recipe)));
    }
    
    private Stream<Pair<IRecipeManager, IRecipe<?>>> streamRecipes() {
        return this.targetedRecipes.stream()
                .map(recipe -> Pair.of(RecipeTypeBracketHandler.getCustomManager(Registry.RECIPE_TYPE.getKey(recipe.getType())), recipe));
    }
    
    private <T extends IInventory, U extends IRecipe<T>> Optional<ActionReplaceRecipe> execute(final IRecipeManager manager, final U recipe, final List<IReplacementRule> rules) {
        try {
            final IRecipeHandler<U> handler = CraftTweakerRegistry.getHandlerFor(recipe);
            final Optional<U> newRecipeMaybe = handler.replaceIngredients(manager, recipe, rules);
            
            if (newRecipeMaybe.isPresent()) {
                return Optional.of(new ActionReplaceRecipe(manager, recipe, newRecipeMaybe.get(), rules));
            }
        } catch (final IRecipeHandler.ReplacementNotSupportedException e) {
            // TODO("Warning maybe?")
            CraftTweakerAPI.logInfo("Unable to replace ingredients in recipe %s: %s", recipe.getId(), e.getMessage());
        } catch (final Throwable t) {
            CraftTweakerAPI.logThrowing("An error has occurred while trying to replace ingredients in recipe %s", t, recipe.getId());
        }
        return Optional.empty();
    }
}
