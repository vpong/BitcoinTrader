import market.Market
import defs._
import scala.collection.mutable.ArrayBuffer

package trader {
  /* A single trader with one strategy
   *
   * The following distinction is made between value and price:
   *    value = how much you can sell something for
   *    price = how much it costs to buy something.
   */
  trait SingleTrader extends Trader { 
    protected var cash: Double
    protected var bitcoins: Double
    val currency: String

    private var _moneyLeft = cash
    protected val initialCash = cash

    private val _history: ArrayBuffer[Transaction] = new ArrayBuffer()

    var _cashLostToRounding: Double = 0
    var _btcLostToRounding: Double = 0

    var _nSells = 0
    var _nBuys = 0
    /*************************
     ** Method to implement **
     *************************/
    // Update the trader so that they know if they want to buy or sell
    def update(): Unit

    // Amount of bitcoins to sell
    def amountToSell: Double

    // Amount of bitcoins to buy
    def amountToBuy: Double

    /* Update the trader after having sold this amount.
     * This shouldn't update the trader's bank information, but rather only
     * decision making state. */
    def updateAfterSell(trans: Transaction): Unit

    /* Update the trader after having bought this amount
     * This shouldn't update the trader's bank information, but rather only
     * decision making state. */
    def updateAfterBuy(trans: Transaction): Unit
    
    /***********************
     ** Protected Methods **
     ***********************/
    // The next 4 methods get a quote for a transaction. The amount bought/sold
    // is in BTC or cash.
    protected def sellQuote(amount: Double): Transaction =
      m.quoteToSell(amount, currency)
    protected def sellQuoteCash(amount: Double): Transaction =
      m.quoteToSellCash(amount, currency)
    protected def buyQuote(amount: Double): Transaction =
      m.quoteToBuy(amount, currency)
    protected def buyQuoteCash(amount: Double): Transaction =
      m.quoteToBuyCash(amount, currency)

    // Returns the amount of money made if [amount] BTCs are sold
    protected def valueOf(amount: Double): Double = sellQuote(amount).dCash

    // Returns the value of 1 BTC
    protected def btcValue: Double = valueOf(1.0)

    // Returns the amount of money needed to buy [amount] BTCs
    protected def priceOf(amount: Double): Double = -buyQuote(amount).dCash

    // Returns the price of 1 BTC
    protected def btcPrice: Double = priceOf(1.0)

    // Returns how many BTCs you would have to sell to get [amount] cash
    protected def bitcoinsCanSellWith(amount: Double): Double =
      -m.quoteToSellCash(amount, currency).dBitcoins

    // Returns how many BTCs you can buy with [amount] cash
    protected def bitcoinsCanBuyWith(amount: Double): Double =
      m.quoteToBuyCash(amount, currency).dBitcoins

    // Returns the maximum amount of BTCs this trader can buy.
    protected def maxBTCsCanBuy: Double = bitcoinsCanBuyWith(cash)

    // Sell [amount] of bitcoins at the market.
    protected def sell(amount: Double): Unit = {
      if (amount <= 0 || amount > bitcoins) {
        if (amount < 0) {
          sys.error(s"Can't sell $amount BTCs - $this")
        }
        return ()
      }
      val trans = m.sell(amount, currency)
      updateBank(trans)
      updateAfterSell(trans)
      _nSells += 1
    }

    // Buy [amount] of bitcoins at the market.
    protected def buy(amount: Double): Unit = {
      if (amount <= 0 || priceOf(amount) > cash) {
        if (amount < 0) {
          sys.error(s"Can't buy $amount BTCs - $this")
        }
        return ()
      }
      val trans = m.buy(amount, currency)
      updateBank(trans)
      updateAfterBuy(trans)
      _nBuys += 1
    }

    /********************
     ** Public Methods **
     ********************/
    /* Update this trader's bank info (cash, bitcoins, history) based on a
     * transaction. */
    def updateBank(trans: Transaction): Unit = {
      val oldBitcoins = bitcoins
      bitcoins = Transaction.roundBtc(bitcoins + trans.dBitcoins)
      if (oldBitcoins + trans.dBitcoins == 0.0 && bitcoins != 0.0) {
        _btcLostToRounding += bitcoins
        bitcoins = 0.0
      }
      if (bitcoins < 0) {
        sys.error(s"Can't have $bitcoins BTCs")
      }

      val oldMax = maxBTCsCanBuy
      cash = Transaction.roundCash(cash + trans.dCash)
      if (oldMax + trans.dCash == 0.0 && cash != 0.0) {
        _cashLostToRounding += cash
        cash = 0.0
      }
      if (cash < 0) {
        sys.error(s"Can't have $cash cash")
      }

      _history append trans
    }

    def getCash: Double = cash
    def getBtc: Double = bitcoins

    def nSells = _nSells
    def nBuys = _nBuys

    // Tell the trader to try trading for one iteration.
    def trade(): Unit = {
      update()
      val toSell = amountToSell
      val toBuy = amountToBuy
      // It's important to get the values before because sell and buy have
      // side effects
      if (toSell > 0 && valueOf(toSell) > 0) sell(toSell)
      if (toBuy > 0 && priceOf(toBuy) > 0) buy(toBuy)
      val btcVal = if (bitcoins > 0 && valueOf(bitcoins) > 0)
          valueOf(bitcoins)
        else
          0.0
      _moneyLeft = cash + btcVal
    }

    def history: TraderHistory = _history.toList

    // Returns the amount of money left (capital not invested + money made)
    def moneyLeft: Double = _moneyLeft

    def cashLostToRounding: Double = _cashLostToRounding
    def btcLostToRounding: Double = _btcLostToRounding
  }

  trait SingleTraderFactory extends TraderFactory {
    def newTrader(
        m: Market,
        cash: Double,
        btc: Double,
        currency: String): SingleTrader
  }
}
