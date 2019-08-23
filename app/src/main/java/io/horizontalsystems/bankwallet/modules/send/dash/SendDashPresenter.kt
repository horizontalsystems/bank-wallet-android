package io.horizontalsystems.bankwallet.modules.send.dash

import io.horizontalsystems.bankwallet.modules.send.SendConfirmationViewItemFactory
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import java.math.BigDecimal

class SendDashPresenter(private val interactor: SendModule.ISendDashInteractor,
                        private val router: SendModule.IRouter,
                        private val confirmationFactory: SendConfirmationViewItemFactory) : SendModule.IViewDelegate, SendModule.ISendDashInteractorDelegate,
        SendAmountModule.IAmountModuleDelegate,
        SendAddressModule.IAddressModuleDelegate {

    var view: SendModule.IView? = null

    private fun syncSendButton() {
        view?.setSendButtonEnabled(enabled = amountModule.validAmount != null && addressModule.address != null)
    }

    private fun syncAvailableBalance() {
        interactor.fetchAvailableBalance(addressModule.address)
    }

    private fun syncFee() {
        interactor.fetchFee(amountModule.coinAmount.value, addressModule.address)
    }

    // SendModule.IViewDelegate

    override lateinit var amountModule: SendAmountModule.IAmountModule

    override lateinit var addressModule: SendAddressModule.IAddressModule

    override lateinit var feeModule: SendFeeModule.IFeeModule

    override fun onViewDidLoad() {
        view?.loadInputItems(listOf(
                SendModule.Input.Amount,
                SendModule.Input.Address,
                SendModule.Input.Fee(false),
                SendModule.Input.SendButton))
    }

    override fun onModulesDidLoad() {
        syncAvailableBalance()
    }

    override fun onAddressScan(address: String) {
        addressModule.didScanQrCode(address)
    }

    override fun onSendClicked() {
        val inputType = amountModule.inputType
        val address = addressModule.address ?: return
        val coinValue = amountModule.coinAmount
        val currencyValue = amountModule.fiatAmount
        val feeCoinValue = feeModule.coinValue
        val feeCurrencyValue = feeModule.currencyValue

        val confirmationViewItem = confirmationFactory.confirmationViewItem(
                inputType,
                address,
                coinValue,
                currencyValue,
                feeCoinValue,
                feeCurrencyValue,
                false
        )

        view?.showConfirmation(confirmationViewItem)
    }

    override fun onSendConfirmed(memo: String?) {
        val amount = amountModule.validAmount ?: return
        val address = addressModule.address ?: return

        interactor.send(amount, address)
    }

    override fun onClear() {
        interactor.clear()
    }

    // SendModule.ISendBitcoinInteractorDelegate

    override fun didSend() {
        router.closeWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view?.showErrorInToast(error)
    }

    override fun didFetchAvailableBalance(availableBalance: BigDecimal) {
        amountModule.setAvailableBalance(availableBalance)
        syncSendButton()
    }

    override fun didFetchFee(fee: BigDecimal) {
        feeModule.setFee(fee)
    }

    // SendAmountModule.ModuleDelegate

    override fun onChangeAmount() {
        syncFee()
        syncSendButton()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.ModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncAvailableBalance()
        syncFee()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

}
