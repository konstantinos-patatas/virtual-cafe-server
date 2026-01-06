/**=========================
    This entity class pairs together the name of
    the customer along with an item ordered.

    why creating such class?
        For storing efficiently customers and their oder item,to be able to
        use any data structure without strange and inefficient data type declarations


    example:
        if the order of alice (order 1 coffee and 1 tea) is being processed, then
        first in waiting area: [coffee for alice],[tea for alice] and then when
        preparing order -> brewing area: [coffee for alice],[tea for alice]
========================**/

package helpers.barista;

public class OrderItem {
    public enum ItemType { TEA, COFFEE } //crate predefined const values

    private final String customerName;
    private final ItemType type;

    public OrderItem(String customerName, ItemType type) {
        this.customerName = customerName;
        this.type = type;
    }

    public String getCustomerName() { return customerName; }
    public ItemType getType() { return type; }

    public int getBrewTimeMs() {
        return (type == ItemType.TEA) ? 30000 : 45000;
    }

    @Override
    public String toString() {
        return type + " for " + customerName;
    }
}
