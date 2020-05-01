package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.core.SingleLiveEvent

class RestoreCoinsView : RestoreCoinsModule.IView {
    val coinsLiveData = MutableLiveData<List<CoinManageViewItem>>()
    val proceedButtonEnabled = MutableLiveData<Boolean>()
    val showDerivationSelectorDialog = SingleLiveEvent<Triple<List<AccountType.Derivation>, AccountType.Derivation, Coin>>()

    override fun setItems(coinViewItems: List<CoinManageViewItem>) {
        coinsLiveData.postValue(coinViewItems)
    }

    override fun setProceedButton(enabled: Boolean) {
        proceedButtonEnabled.postValue(enabled)
    }

    override fun showDerivationSelectorDialog(derivationOptions: List<AccountType.Derivation>, selected: AccountType.Derivation, coin: Coin) {
        showDerivationSelectorDialog.postValue(Triple(derivationOptions, selected, coin))
    }

}
