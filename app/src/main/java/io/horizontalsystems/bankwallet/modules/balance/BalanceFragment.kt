package io.horizontalsystems.bankwallet.modules.balance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.ui.dialogs.BalanceSortDialogFragment
import io.horizontalsystems.bankwallet.ui.dialogs.ManageKeysDialog
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_balance.*

class BalanceFragment : Fragment(), BalanceCoinAdapter.Listener, BalanceSortDialogFragment.Listener, ReceiveFragment.Listener {

    private lateinit var viewModel: BalanceViewModel
    private lateinit var coinAdapter: BalanceCoinAdapter
    private var menuSort: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(BalanceViewModel::class.java)
        viewModel.init()
        coinAdapter = BalanceCoinAdapter(this, viewModel.delegate)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        recyclerCoins.adapter = coinAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        sortButton.setOnClickListener {
            viewModel.delegate.onSortClick()
        }

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }

        observeLiveData()
        setSwipeBackground()
    }

    override fun onResume() {
        super.onResume()
        coinAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    // BalanceSort listener

    override fun onSortItemSelect(sortType: BalanceSortType) {
        viewModel.delegate.onSortTypeChanged(sortType)
    }

    private fun setSwipeBackground() {
        activity?.theme?.let { theme ->
            LayoutHelper.getAttr(R.attr.SwipeRefreshBackgroundColor, theme)?.let { color ->
                pullToRefresh.setProgressBackgroundColorSchemeColor(color)
            }
            LayoutHelper.getAttr(R.attr.ColorOz, theme)?.let { color ->
                pullToRefresh.setColorSchemeColors(color)
            }
        }
    }

    // ReceiveFragment listener
    override fun shareReceiveAddress(address: String) {
        ShareCompat.IntentBuilder
                .from(activity)
                .setType("text/plain")
                .setText(address)
                .startChooser()
    }

    // BalanceAdapter listener

    override fun onSendClicked(position: Int) {
        viewModel.onSendClicked(position)
    }

    override fun onReceiveClicked(position: Int) {
        viewModel.onReceiveClicked(position)
    }

    override fun onItemClick(position: Int) {
        coinAdapter.toggleViewHolder(position)
    }

    override fun onAddCoinClick() {
        viewModel.delegate.openManageCoins()
    }

    override fun onClickChart(position: Int) {
        viewModel.delegate.openChart(position)
    }

    // LiveData

    private fun observeLiveData() {
        viewModel.openReceiveDialog.observe(viewLifecycleOwner, Observer { wallet ->
            ReceiveFragment(wallet, this).also { it.show(childFragmentManager, it.tag) }
        })

        viewModel.openSendDialog.observe(viewLifecycleOwner, Observer {
            (activity as? MainActivity)?.openSend(it)
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            pullToRefresh.isRefreshing = false
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ManageWalletsModule.start(it) }
        })

        viewModel.reloadLiveEvent.observe(viewLifecycleOwner, Observer {
            coinAdapter.notifyDataSetChanged()
            reloadHeader()
            sortButton?.visibility = if (viewModel.delegate.itemsCount >= 5) View.VISIBLE else View.GONE
            if (viewModel.delegate.itemsCount > 0) {
                recyclerCoins.animate().alpha(1f)
            }
        })

        viewModel.reloadHeaderLiveEvent.observe(viewLifecycleOwner, Observer {
            reloadHeader()
        })

        viewModel.reloadItemLiveEvent.observe(viewLifecycleOwner, Observer { position ->
            coinAdapter.notifyItemChanged(position)
        })

        viewModel.openSortingTypeDialogLiveEvent.observe(viewLifecycleOwner, Observer { sortingType ->
            BalanceSortDialogFragment.newInstance(this, sortingType).also { it.show(childFragmentManager, it.tag) }
        })

        viewModel.setSortingOnLiveEvent.observe(viewLifecycleOwner, Observer { visible ->
            menuSort?.isVisible = visible
        })

        viewModel.showBackupAlert.observe(viewLifecycleOwner, Observer {(coin, predefinedAccount) ->
            activity?.let { activity ->
                val title = getString(R.string.ManageKeys_Delete_Alert_Title)
                val subtitle = getString(predefinedAccount.title)
                val description = getString(R.string.ManageKeys_Delete_Alert)
                ManageKeysDialog.show(title, subtitle, description, activity, object : ManageKeysDialog.Listener {
                    override fun onClickBackupKey() {
                        viewModel.delegate.openBackup()
                    }
                }, ManageKeysDialog.ManageAction.BACKUP)
            }
        })

        viewModel.openBackup.observe(viewLifecycleOwner, Observer { (account, coinCodesStringRes) ->
            context?.let { BackupModule.start(it, account, getString(coinCodesStringRes)) }
        })

        viewModel.openChartModule.observe(viewLifecycleOwner, Observer { coin ->
            RateChartFragment(coin).also { it.show(childFragmentManager, it.tag) }
        })

    }

    private fun reloadHeader() {
        val headerViewItem = viewModel.delegate.getHeaderViewItem()

        context?.let {
            val color = if (headerViewItem.upToDate) R.color.yellow_d else R.color.yellow_50
            balanceText.setTextColor(ContextCompat.getColor(it, color))
        }

        balanceText.text = headerViewItem.currencyValue?.let {
            App.numberFormatter.format(it)
        }
    }
}
