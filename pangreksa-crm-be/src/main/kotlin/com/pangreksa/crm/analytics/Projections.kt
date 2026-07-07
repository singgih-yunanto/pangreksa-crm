package com.pangreksa.crm.analytics

import java.math.BigDecimal

/** A labelled count bucket (e.g. leads per status, tasks per status). */
interface Bucket {
    val label: String?
    val sortOrder: Int?
    val count: Long
}

/** A labelled bucket carrying a money total (e.g. deals per stage). */
interface AmountBucket {
    val label: String?
    val sortOrder: Int?
    val count: Long
    val amount: BigDecimal?
}

/** Per-owner sales rollup. */
interface OwnerSales {
    val owner: String?
    val count: Long
    val total: BigDecimal?
    val won: BigDecimal?
}

/** Scalar deal summary aggregates. */
interface DealSummary {
    val openCount: Long
    val wonCount: Long
    val totalCount: Long
    val pipeline: BigDecimal?
    val expected: BigDecimal?
}
