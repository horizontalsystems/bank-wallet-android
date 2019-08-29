package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.core.managers.ServiceExchangeApi.HostType
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.RateData
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.math.BigDecimal

class RateStatsManager(private val networkManager: INetworkManager, private val rateStorage: IRateStorage) {

    private val disposables = CompositeDisposable()
    private val cache = mutableMapOf<StatsKey, StatsData>()

    fun getRateStats(coinCode: String, currencyCode: String): Flowable<StatsData> {
        val statsKey = StatsKey(coinCode, currencyCode)
        val cached = cache[statsKey]
        if (cached != null) {
            return Flowable.just(cached)
        }

        val rateLocal = rateStorage.latestRateObservable(coinCode, currencyCode)
        val rateStats = networkManager
                .getRateStats(HostType.MAIN, coinCode, currencyCode)
                .onErrorResumeNext(networkManager.getRateStats(HostType.FALLBACK, coinCode, currencyCode))

        return Flowable.zip(rateLocal, rateStats, BiFunction<Rate, RateStatData, Pair<Rate, RateStatData>> { a, b -> Pair(a, b) })
                .map { (rate, data) ->
                    val stats = mutableMapOf<String, ChartData>()
                    val diffs = mutableMapOf<String, BigDecimal>()

                    for (type in data.stats.keys) {
                        val statsData = data.stats[type] ?: continue
                        val chartType = ChartType.fromString(type) ?: continue
                        val chartData = convert(statsData, rate, chartType)

                        stats[type] = chartData
                        diffs[type] = growthDiff(chartData.points)
                    }

                    StatsData(data.marketCap, stats, diffs).also { cache[statsKey] = it }
                }
    }

    fun clear() {
        disposables.clear()
        cache.clear()
    }

    private fun convert(data: RateData, rate: Rate?, chartType: ChartType): ChartData {
        val rates = data.rates.toMutableList()
        if (rate != null) {
            rates.add(rate.value.toFloat())
        }

        val points = when (chartType) {
            ChartType.MONTHLY18 -> rates.takeLast(53) // for one year
            else -> data.rates
        }

        return ChartData(points, data.timestamp, data.scale, chartType)
    }

    private fun growthDiff(points: List<Float>): BigDecimal {
        val pointStart = points.first { it != 0f }
        val pointEnd = points.last()

        return ((pointEnd - pointStart) / pointStart * 100).toBigDecimal()
    }
}

data class StatsKey(val coinCode: String, val currencyCode: String)
data class StatsData(val marketCap: BigDecimal, val stats: Map<String, ChartData>, val diff: Map<String, BigDecimal>)
