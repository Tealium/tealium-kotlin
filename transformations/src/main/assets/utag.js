var utag = {
    DB: function (m) {
        console.log(m)
    },
    extend: {
        "blr": [],
        "alr": [function (a, b) {
            try {
                if (1) {
                    b['event_source'] = 'tag_managment'
                }
            } catch (e) {
                //        utag.DB(e);

            }
        }
            , function (a, b) {
                try {
                    if (1) {
                        var stored = storage.read("key") || 0;
                        console.log("Storage value is: " + stored);
                        storage.save("key", ++stored, null);
                    }
                } catch (e) {
                    //        utag.DB(e)
                }
            }
            , function (a, b, c, d) {
                b._ccity = (typeof b['customer_city'] != 'undefined') ? b['customer_city'] : '';
                b._ccountry = (typeof b['customer_country'] != 'undefined') ? b['customer_country'] : '';
                b._ccurrency = (typeof b['order_currency'] != 'undefined') ? b['order_currency'] : '';
                b._ccustid = (typeof b['customer_id'] != 'undefined') ? b['customer_id'] : '';
                b._corder = (typeof b['order_id'] != 'undefined') ? b['order_id'] : '';
                b._cpromo = (typeof b['order_coupon_code'] != 'undefined') ? b['order_coupon_code'] : '';
                b._cship = (typeof b['order_shipping'] != 'undefined') ? b['order_shipping'] : '';
                b._cstate = (typeof b['customer_state'] != 'undefined') ? b['customer_state'] : '';
                b._cstore = (typeof b['order_store'] != 'undefined') ? b['order_store'] : 'web';
                b._csubtotal = (typeof b['order_subtotal'] != 'undefined') ? b['order_subtotal'] : '';
                b._ctax = (typeof b['order_tax'] != 'undefined') ? b['order_tax'] : '';
                b._ctotal = (typeof b['order_total'] != 'undefined') ? b['order_total'] : '';
                b._ctype = (typeof b['order_type'] != 'undefined') ? b['order_type'] : '';
                b._czip = (typeof b['customer_zip'] != 'undefined') ? b['customer_zip'] : '';
                b._cprod = (typeof b['product_id'] != 'undefined' && b['product_id'].length > 0) ? b['product_id'].split(',') : [];
                b._cprodname = (typeof b['product_name'] != 'undefined' && b['product_name'].length > 0) ? b['product_name'].split(',') : [];
                b._cbrand = (typeof b['product_brand'] != 'undefined' && b['product_brand'].length > 0) ? b['product_brand'].split(',') : [];
                b._ccat = (typeof b['product_category'] != 'undefined' && b['product_category'].length > 0) ? b['product_category'].split(',') : [];
                b._ccat2 = (typeof b['product_subcategory'] != 'undefined' && b['product_subcategory'].length > 0) ? b['product_subcategory'].split(',') : [];
                b._cquan = (typeof b['product_quantity'] != 'undefined' && b['product_quantity'].length > 0) ? b['product_quantity'].split(',') : [];
                b._cprice = (typeof b['product_unit_price'] != 'undefined' && b['product_unit_price'].length > 0) ? b['product_unit_price'].split(',') : [];
                b._csku = (typeof b['product_sku'] != 'undefined' && b['product_sku'].length > 0) ? b['product_sku'].split(',') : [];
                b._cpdisc = (typeof b['product_discount'] != 'undefined' && b['product_discount'].length > 0) ? b['product_discount'].split(',') : [];
                if (b._cprod.length == 0) {
                    b._cprod = b._csku.slice()
                }
                ; if (b._cprodname.length == 0) {
                    b._cprodname = b._csku.slice()
                }
                ; function tf(a) {
                    if (a == '' || isNaN(parseFloat(a))) {
                        return a
                    } else {
                        return (parseFloat(a)).toFixed(2)
                    }
                }
                ; b._ctotal = tf(b._ctotal);
                b._csubtotal = tf(b._csubtotal);
                b._ctax = tf(b._ctax);
                b._cship = tf(b._cship);
                for (c = 0; c < b._cprice.length; c++) {
                    b._cprice[c] = tf(b._cprice[c])
                }
                ; for (c = 0; c < b._cpdisc.length; c++) {
                    b._cpdisc[c] = tf(b._cpdisc[c])
                }
                ;
            }
        ],
        "firebaseAnalytics" : [
            function (a, b) {
                try {
                    if (1) {
                        b['firebase_api'] = '12345'
                    }
                } catch (e) {
                    //        utag.DB(e);
                }
            }
        ],
        "localjsoncommand": [
            function (a, b) {
                try {
                    if (1) {
                        b['hello'] = 'world'
                    }
                } catch (e) {
                    //        utag.DB(e);
                }
            }
        ]
    },
    transform: function (a, b, c) {
        if (typeof b === "string") {
            b = JSON.parse(b);
        }
        // else -> assume object

        var extend = utag.extend || {};
        var scope = extend[c || ""] || [];

        for (i = 0; i < scope.length; i++) {
            scope[i](a, b)
        }
        return JSON.stringify(b);
    },
    transformJson: function (a, b, c) {
            if (typeof b === "string") {
                b = JSON.parse(b);
            }
            // else -> assume object

            var extend = utag.extend || {};
            var scope = extend[c || ""] || [];

            for (i = 0; i < scope.length; i++) {
                scope[i](a, b)
            }
            return b;
        }
};
