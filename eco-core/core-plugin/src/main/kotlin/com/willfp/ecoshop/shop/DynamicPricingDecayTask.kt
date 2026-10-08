package com.willfp.ecoshop.shop

class DynamicPricingDecayTask : Runnable {
    override fun run() {
        for (item in ShopItems.values()) {
            if (item.hasDynamicActivity()) item.applyDecay()
        }
    }
}
