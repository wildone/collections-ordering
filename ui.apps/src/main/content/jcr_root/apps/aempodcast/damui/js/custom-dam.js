$(function() {
    $(document).on("mouseup", "button.u-coral-openHand", function(e) {
        var collectionPath = "";

        if(location.href.indexOf("collectiondetails.html") !== -1) {
            collectionPath = $(".foundation-content-path").data("foundation-content-path");
        } else {
            collectionPath = $(".content-fragment-collection-list .is-active .trash").data("coll");
        }

        window.setTimeout(function() {
            var items = $("coral-table table tr");

            var paths = [];

            for(var i = 0; i < items.length; i++) {
                var item = items[i];
                if($(item).hasClass("foundation-collection-item")) {
                    paths.push($(item).data("foundation-collection-item-id"));
                }
            }

            var orderingData = {};
            orderingData.collectionPath = collectionPath;
            orderingData.order = paths;

            $.ajax({
                url: "/bin/dam/collection/order",
                type: 'POST',
                data: { collectionPath: orderingData.collectionPath, paths: JSON.stringify(orderingData.order) }
            });
        }, 200);
    });
});