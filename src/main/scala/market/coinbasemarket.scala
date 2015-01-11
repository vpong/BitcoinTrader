import defs._
import utils._
import scala.io.Source
import com.coinbase.api.entity.Transfer
import com.coinbase.api.Coinbase
import com.coinbase.api.CoinbaseBuilder
import org.joda.money.Money
import scala.collection.JavaConversions._

package market { 
  // A wrapper around the coinbase java API
  object CoinbaseMarket extends Market {
    val Currency = "USD"
    var cb: Coinbase = _ // the CoinbaseBuilder
    var _isOpen = false
    val fakeTrans = new Transaction(0, 0, 0, "USD")

    def getBTCs(): Double = 0.0
    def getCash(): Double = 0.0

    def sell(amount: Double, currency: String): Transaction = fakeTrans

    def buy(amount: Double, currency: String): Transaction = fakeTrans

    def quoteToSell(amount: Double, currency: String): Transaction = {
      val q = cb.getSellQuote(Money.parse(s"BTC $amount"))
      /*
      val fees = q.getFees()
      println(s"Sell quote fees = $fees")
      println(s"subtotal = ${q.getSubtotal()}")
      println(s"total = ${q.getTotal()}")
      */
      new Transaction(
        -amount,
        q.getTotal().getAmount().doubleValue(),
        time(),
        currency)
    }

    def quoteToBuy(amount: Double, currency: String): Transaction = {
      val q = cb.getBuyQuote(Money.parse(s"BTC $amount"))
      new Transaction(
        amount,
        -q.getTotal().getAmount().doubleValue(),
        time(),
        currency)
    }

    def quoteToSellCash(amount: Double, currency: String): Transaction =
      fakeTrans

    def quoteToBuyCash(amount: Double, currency: String): Transaction =
      fakeTrans

    def update(): Unit = ()

    def isOpen(): Boolean = _isOpen

    def open: Unit = ()
      val secretData =
        (for(l <- Source.fromFile(".secret").getLines()) yield l).toList
      val key = secretData.head.split("=").last
      val secret = secretData.last.split("=").last
      cb = new CoinbaseBuilder()
                          .withApiKey(key, secret)
                          .build()
      //val r = cb.getTransactions()
      //println(r.getTotalCount())
      _isOpen = true

    def history: MarketHistory = {
      for (p <- cb.getHistoricalPrices().toList) yield {
        new BitcoinStat(
          p.getTime().getMillis(),
          p.getSpotPrice().getAmount().doubleValue())
      }
    }

    def tradeHistory: TraderHistory = {
      for (t <- cb.getTransfers().getTransfers().toList) yield {
        if (t.getType().toString() == Transfer.Type.SELL.toString()) {
          new Transaction(
            -t.getBtc().getAmount().doubleValue(),
            t.getTotal().getAmount().doubleValue(),
            t.getPayoutDate().getMillis(),
            Currency)
        } else { // bought BTC
          new Transaction(
            t.getBtc().getAmount().doubleValue(),
            -t.getTotal().getAmount().doubleValue(),
            t.getPayoutDate().getMillis(),
            Currency)
        }
      }
    }

    def reset(): Unit = ()

    override def toString = "Coinbase Market"
  }
}
