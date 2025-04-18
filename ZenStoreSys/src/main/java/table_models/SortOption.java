package table_models;

public class SortOption {
    private String name;
    private String type; // "category" or "date"
    private Integer categoryId; // only used for category type

    public SortOption(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public SortOption(String name, String type, Integer categoryId) {
        this.name = name;
        this.type = type;
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    @Override
    public String toString() {
        return name;
    }
}