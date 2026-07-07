package com.pangreksa.crm.analytics.web

import java.math.BigDecimal

data class SummaryDto(
    val pipelineValue: BigDecimal,
    val expectedRevenue: BigDecimal,
    val winRate: Int,
    val openDeals: Long,
    val wonDeals: Long,
    val openLeads: Long,
)

data class BucketDto(val label: String, val count: Long, val amount: BigDecimal? = null)

data class OwnerSalesDto(
    val owner: String,
    val count: Long,
    val total: BigDecimal,
    val won: BigDecimal,
    val open: BigDecimal,
)

data class LeadStatsDto(val byStatus: List<BucketDto>, val converted: Long)

data class ActivityStatsDto(
    val tasks: Long,
    val meetings: Long,
    val calls: Long,
    val tasksByStatus: List<BucketDto>,
)
