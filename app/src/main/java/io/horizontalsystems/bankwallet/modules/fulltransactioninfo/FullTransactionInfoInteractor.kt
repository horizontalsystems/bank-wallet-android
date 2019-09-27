package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.managers.TransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FullTransactionInfoInteractor(private val transactionInfoFactory: FullTransactionInfoFactory, private var dataProviderManager: TransactionDataProviderManager, private val clipboardManager: TextHelper)
    : FullTransactionInfoModule.Interactor, FullTransactionInfoModule.ProviderDelegate {

    val disposables = CompositeDisposable()
    var delegate: FullTransactionInfoModule.InteractorDelegate? = null

    private var provider: FullTransactionInfoModule.FullProvider? = null

    //
    // IInteractor implementations
    //
    override fun didLoad() {
        disposables.add(dataProviderManager.baseProviderUpdatedSignal.subscribe {
            delegate?.onProviderChange()
        })
    }

    override fun updateProvider(wallet: Wallet) {
        provider = transactionInfoFactory.providerFor(wallet)
    }

    override fun url(hash: String): String? {
        return provider?.url(hash)
    }

    override fun retrieveTransactionInfo(transactionHash: String) {
        provider?.let {
            disposables.add(it.retrieveTransactionInfo(transactionHash)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        delegate?.onReceiveTransactionInfo(it)
                    }, {
                        delegate?.onError(provider?.providerName)
                    })
            )
        }
    }

    override fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

    override fun clear() {
        disposables.clear()
    }

    //
    // ProviderDelegate implementations
    //
    override fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord) {
        delegate?.onReceiveTransactionInfo(transactionRecord)
    }
}
