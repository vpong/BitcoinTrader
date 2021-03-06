import defs._

package market { 
  trait Market {
    protected var _spotPrice: Option[Double]
    // Get the last spot price saved: $ per BTC
    def spotPrice: Double = _spotPrice match {
      case Some(p) => p
      case None => sys.error("Market is not open. Try calling open()" +
        " and then calling update().")
    }

    // Sell [amount] of bitcoints. Returns the amount sucessfully sold.
    def sell(amount: Double, currency: String): Transaction

    // Buy [amount] of bitcoints. Returns the amount sucessfully bought.
    def buy(amount: Double, currency: String): Transaction

    // Get a predicted transaction if you were to sell [amount] BTCs
    def quoteToSell(amount: Double, currency: String): Transaction

    // Get a predicted transaction if you were to buy [amount] BTCs
    def quoteToBuy(amount: Double, currency: String): Transaction

    // Get a predicted transaction if you were to sell [amount] worth of BTC
    def quoteToSellCash(amount: Double, currency: String): Transaction

    // Get a predicted transaction if you were to buy [amount] worth of BTC
    def quoteToBuyCash(amount: Double, currency: String): Transaction

    // Update the information about this market. Call this before other methods
    // if the market just opened up.
    def update(): Unit

    // Returns true iff the market is still open. If not, do not call any other
    // method besides open()
    def isOpen: Boolean

    // Open up the market for the first time (i.e. initialize things.)
    def open(): Unit

    // History of all the bitcoin statistics
    def history: MarketHistory

    // Reset the market as if we had never used it before
    def reset(): Unit
  }
}
