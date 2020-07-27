package com.tealium.remotecommanddispatcher

import com.tealium.dispatcher.EventDispatch
import junit.framework.Assert
import org.junit.Test

class RemoteCommandParserTests {

    @Test
    fun mapDispatchValidResultantMap() {
        val dispatch = EventDispatch(
                "test_dispatch",
                mapOf("product_id" to arrayListOf("ABC123"),
                        "product_name" to arrayListOf("milk chocolate"),
                        "product_brand" to arrayListOf("see's"),
                        "product_unit_price" to arrayListOf("19.99"),
                        "product_quantity" to arrayListOf("1"),
                        "customer_id" to "CUST234324",
                        "customer_full_name" to "Test Name",
                        "order_id" to "ORD239847",
                        "event_title" to "ecommerce_purchase",
                        "order_tax_amount" to "1.99",
                        "order_shipping_amount" to "5.00"))
        val lookup = mapOf("campaign_keywords" to "cp1",
                "campaign" to "campaign",
                "checkout_option" to "checkout_option",
                "checkout_step" to "checkout_step",
                "content" to "content",
                "content_type" to "content_type",
                "coupon" to "coupon",
                "product_brand" to "item_brand",
                "product_category" to "item_category",
                "product_id" to "item_id",
                "product_list" to "item_list",
                "product_location_id" to "item_location_id",
                "product_name" to "item_name",
                "product_variant" to "item_variant",
                "campaign_medium" to "medium",
                "product_unit_price" to "price",
                "product_quantity" to "quantity",
                "search_keyword" to "search_term",
                "order_shipping_amount" to "shipping",
                "order_tax_amount" to "tax",
                "order_id" to "transaction_id",
                "order_total" to "value",
                "event_title" to "event_name",
                "tealium_event" to "command_name",
                "customer_id" to "user_id")

        val expectedMap = mapOf("item_id" to arrayListOf("ABC123"), "item_name" to arrayListOf("milk chocolate"), "item_brand" to arrayListOf("see's"), "price" to arrayListOf("19.99"), "quantity" to arrayListOf("1"), "user_id" to "CUST234324", "transaction_id" to "ORD239847", "event_name" to "ecommerce_purchase", "tax" to "1.99", "shipping" to "5.00", "command_name" to "test_dispatch")
        val testOutput = RemoteCommandParser.mapDispatch(dispatch, lookup)

        Assert.assertEquals(expectedMap, testOutput)
        Assert.assertTrue(testOutput.containsKey("command_name"))
        Assert.assertEquals(dispatch.eventName, testOutput["command_name"])
    }

    @Test
    fun mapDispatchWithDotParamsResultantMap() {
        val dispatch = EventDispatch(
                "test_dot_dispatch",
                mapOf("content" to "someContent",
                        "content_type" to "someContentType",
                        "coupon" to "someCoupon",
                        "product_brand" to arrayListOf("someBrand"),
                        "product_category" to arrayListOf("someCategory"),
                        "product_id" to arrayListOf("someId"),
                        "product_list" to arrayListOf("someList"),
                        "product_location_id" to "someLocationId",
                        "product_name" to arrayListOf("someName"),
                        "product_variant" to arrayListOf("someVariant"),
                        "campaign_medium" to "someMedium",
                        "product_unit_price" to arrayListOf(0.00),
                        "product_quantity" to arrayListOf(1),
                        "search_keyword" to "someSearchTerm",
                        "order_shipping_amount" to 5.00,
                        "order_tax_amount" to 3.00,
                        "order_id" to "ABC123",
                        "order_total" to 20.00,
                        "event_title" to "order",
                        "tealium_event" to "purchase",
                        "customer_id" to "cust1234"))
        val lookup = mapOf("content" to "content",
                "content_type" to "content_type",
                "coupon" to "purchase.coupon",
                "product_brand" to "event.item_brand",
                "product_category" to "event.item_category",
                "product_id" to "event.item_id",
                "product_list" to "event.item_list",
                "product_location_id" to "event.item_location_id",
                "product_name" to "event.item_name",
                "product_variant" to "event.item_variant",
                "campaign_medium" to "event.medium",
                "product_unit_price" to "event.price",
                "product_quantity" to "event.quantity",
                "search_keyword" to "event.search_term",
                "order_shipping_amount" to "purchase.shipping",
                "order_tax_amount" to "purchase.tax",
                "order_id" to "purchase.transaction_id",
                "order_total" to "purchase.value",
                "event_title" to "event_name",
                "tealium_event" to "command_name",
                "customer_id" to "user.user_id")

//        val expectedMap = mapOf()
        val testOutput = RemoteCommandParser.mapDispatch(dispatch, lookup)

        println(testOutput)
        Assert.assertTrue(testOutput.containsKey("command_name"))
        Assert.assertEquals("purchase", testOutput["command_name"])
    }

}