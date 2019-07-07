package com.blamejared.crafttweaker.impl.item;

import com.blamejared.crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.text.StringTextComponent;

public class MCItemStackMutable implements IItemStack {
    
    private final ItemStack internal;
    
    public MCItemStackMutable(ItemStack internal) {
        this.internal = internal;
    }
    
    @Override
    public boolean isEmpty() {
        return internal.isEmpty();
    }
    
    @Override
    public String getDisplayName() {
        return internal.getDisplayName().getFormattedText();
    }
    
    @Override
    public IItemStack setDisplayName(String name) {
        internal.setDisplayName(new StringTextComponent(name));
        return this;
    }
    
    @Override
    public int getAmount() {
        return internal.getCount();
    }
    
    @Override
    public IItemStack setAmount(int amount) {
        internal.setCount(amount);
        return this;
    }
    
    @Override
    public IItemStack withDamage(int damage) {
        internal.setDamage(damage);
        return this;
    }
    
    @Override
    public boolean isStackable() {
        return internal.isStackable();
    }
    
    @Override
    public boolean isDamageable() {
        return internal.isDamageable();
    }
    
    @Override
    public boolean isDamaged() {
        return internal.isDamaged();
    }
    
    @Override
    public int getMaxDamage() {
        return internal.getMaxDamage();
    }
    
    @Override
    public String getTranslationKey() {
        return internal.getTranslationKey();
    }
    
    @Override
    public String getCommandString() {
        final StringBuilder sb = new StringBuilder("<item:");
        sb.append(internal.getItem().getRegistryName());
        sb.append(">");
        if(internal.getDamage() > 0)
            sb.append(".withDamage(").append(internal.getDamage()).append(")");
        
        if(getAmount() != 1)
            sb.append(" * ").append(getAmount());
        return sb.toString();
    }
    
    @Override
    public IItemStack[] getItems() {
        return new IItemStack[]{this};
    }
    
    @Override
    public ItemStack getInternal() {
        return internal;
    }
    
    @Override
    public boolean matches(IItemStack stack) {
        ItemStack stack1 = internal;
        ItemStack stack2 = stack.getInternal();
        
        if(stack1.isEmpty() != stack2.isEmpty()){
            return false;
        }
        if(stack1.getItem() != stack2.getItem()){
            return false;
        }
        if(stack1.getCount() > stack2.getCount()){
            return false;
        }
        if(stack1.getDamage() != stack2.getDamage()){
            return false;
        }
        return true;
    }
    
    @Override
    public Ingredient asVanillaIngredient() {
        return Ingredient.fromStacks(getInternal());
    }
    
    @Override
    public String toString() {
        return getCommandString();
    }
}