package com.zive.moneymanager.data.db.entity

/**
 * Consumables get used up and can expire; assets are kept and can sit unused.
 * The distinction drives which fields matter and which declutter rules apply.
 */
enum class ItemKind(val label: String) {
    CONSUMABLE("Consumable"),
    ASSET("Asset"),
}

enum class ItemStatus(val label: String) {
    IN_STOCK("In stock"),
    TO_DONATE("To donate"),
    TO_DISCARD("To throw away"),
    USED_UP("Used up"),
    DONATED("Donated"),
    DISCARDED("Thrown away"),
    SOLD("Sold");

    /** True while the thing is still in your home and counts toward clutter. */
    val isHeld: Boolean get() = this == IN_STOCK || this == TO_DONATE || this == TO_DISCARD

    /** True once it has left — these rows are history, not inventory. */
    val isResolved: Boolean get() = !isHeld

    /** Queued on the donate / throw-away checklist but not yet dealt with. */
    val isPendingDisposal: Boolean get() = this == TO_DONATE || this == TO_DISCARD
}

enum class QuantityUnit(val label: String) {
    PIECE("pcs"),
    PACK("pack"),
    BOX("box"),
    BOTTLE("bottle"),
    CAN("can"),
    GRAM("g"),
    KILOGRAM("kg"),
    MILLILITRE("ml"),
    LITRE("L"),
    PAIR("pair"),
    SET("set"),
}
