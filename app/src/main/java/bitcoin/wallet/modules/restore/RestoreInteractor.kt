package bitcoin.wallet.modules.restore

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.managers.WordsManager

class RestoreInteractor(private val wordsManager: WordsManager, private val adapterManager: AdapterManager, private val keystoreSafeExecute: IKeyStoreSafeExecute) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun restore(words: List<String>) {

        keystoreSafeExecute.safeExecute(
                action = Runnable { wordsManager.restore(words) },
                onSuccess = Runnable {
                    adapterManager.initAdapters(words)
                    wordsManager.wordListBackedUp = true
                    delegate?.didRestore()
                },
                onFailure = Runnable { delegate?.didFailToRestore() }
        )
    }
}
