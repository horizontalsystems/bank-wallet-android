package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.top.MarketListFavoritesDataSource
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopService
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopViewModel

object MarketFavoritesModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketTopService(App.currencyManager, MarketListFavoritesDataSource(App.xRateManager, App.marketFavoritesManager))
            return MarketTopViewModel(service, listOf(service)) as T
        }

    }

}

