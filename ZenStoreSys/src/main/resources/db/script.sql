create table admin
(
    admin_id  int auto_increment
        primary key,
    full_name varchar(50)  null,
    username  varchar(20)  null,
    password  varchar(200) null
);

create table categories
(
    category_id   int auto_increment
        primary key,
    category_name varchar(100) not null,
    constraint category_name
        unique (category_name)
);

create table customers
(
    customer_id    int auto_increment
        primary key,
    name           varchar(100)                               not null,
    credit_balance decimal(10, 2) default 0.00                null,
    phone          varchar(20)                                null,
    email          varchar(100)                               null,
    created_at     timestamp      default current_timestamp() not null,
    constraint name
        unique (name)
);

create table credit_transactions
(
    transaction_id   int auto_increment
        primary key,
    customer_id      int                                   not null,
    amount           decimal(10, 2)                        not null,
    transaction_date timestamp default current_timestamp() not null,
    status           enum ('PAID', 'UNPAID')               not null,
    due_date         date                                  null,
    sale_id          int                                   null,
    constraint credit_transactions_ibfk_1
        foreign key (customer_id) references customers (customer_id)
            on delete cascade
);

create index customer_id
    on credit_transactions (customer_id);

create table products
(
    product_id        int auto_increment
        primary key,
    image_path        varchar(255)                               null,
    name              varchar(200)                               not null,
    category_id       int                                        not null,
    cost_price        decimal(10, 2) default 0.00                not null,
    markup_percentage decimal(5, 2)                              not null,
    stock             int            default 0                   not null,
    selling_price     decimal(10, 2)                             not null,
    barcode_path      varchar(255)                               null,
    last_restock      timestamp      default current_timestamp() not null,
    constraint barcode_path
        unique (barcode_path),
    constraint products_ibfk_1
        foreign key (category_id) references categories (category_id)
            on delete cascade
);

create table discounts
(
    discount_id    int auto_increment
        primary key,
    product_id     int                                          null,
    category_id    int                                          null,
    discount_type  enum ('PERCENTAGE', 'FIXED', 'BOGO', 'BULK') not null,
    discount_value decimal(10, 2)                               not null,
    min_quantity   int        default 1                         null,
    start_date     datetime                                     not null,
    end_date       datetime                                     not null,
    is_active      tinyint(1) default 1                         null,
    constraint discounts_ibfk_1
        foreign key (product_id) references products (product_id),
    constraint discounts_ibfk_2
        foreign key (category_id) references categories (category_id)
);

create index category_id
    on discounts (category_id);

create index product_id
    on discounts (product_id);

create trigger set_discount_active_on_insert
    before insert
    on discounts
    for each row
BEGIN
    IF NEW.start_date > CURRENT_DATE THEN
        SET NEW.is_active = 0;
    ELSEIF NEW.start_date <= CURRENT_DATE AND NEW.end_date >= CURRENT_DATE THEN
        SET NEW.is_active = 1;
    ELSE
        SET NEW.is_active = 0;
    END IF;
END;

create trigger set_discount_active_on_update
    before update
    on discounts
    for each row
BEGIN
    IF NEW.start_date > CURRENT_DATE THEN
        SET NEW.is_active = 0;
    ELSEIF NEW.start_date <= CURRENT_DATE AND NEW.end_date >= CURRENT_DATE THEN
        SET NEW.is_active = 1;
    ELSE
        SET NEW.is_active = 0;
    END IF;
END;

create table low_stock_alerts
(
    alert_id    int auto_increment
        primary key,
    product_id  int                                   not null,
    stock_level int                                   not null,
    alert_date  timestamp default current_timestamp() not null,
    constraint low_stock_alerts_ibfk_1
        foreign key (product_id) references products (product_id)
            on delete cascade
);

create index product_id
    on low_stock_alerts (product_id);

create table product_prices
(
    price_id    int auto_increment
        primary key,
    product_id  int                                   not null,
    old_price   decimal(10, 2)                        not null,
    new_price   decimal(10, 2)                        not null,
    change_date timestamp default current_timestamp() not null,
    constraint product_prices_ibfk_1
        foreign key (product_id) references products (product_id)
            on delete cascade
);

create index product_id
    on product_prices (product_id);

create index category_id
    on products (category_id);

create table sales
(
    sale_id     int auto_increment
        primary key,
    total_price decimal(10, 2)                        not null,
    sale_date   timestamp default current_timestamp() not null
);

create table sales_items
(
    sale_item_id int auto_increment
        primary key,
    sale_id      int            not null,
    product_id   int            not null,
    quantity     int            not null,
    subtotal     decimal(10, 2) not null,
    final_price  decimal(10, 2) not null,
    constraint sales_items_ibfk_1
        foreign key (sale_id) references sales (sale_id)
            on delete cascade,
    constraint sales_items_ibfk_2
        foreign key (product_id) references products (product_id)
            on delete cascade
);

create index product_id
    on sales_items (product_id);

create index sale_id
    on sales_items (sale_id);

create view lowstockproducts as
select `p`.`product_id` AS `product_id`, `p`.`name` AS `name`, `p`.`stock` AS `stock`
from `z_store_db`.`products` `p`
where `p`.`stock` <= 5;

create procedure GetAllProducts()
BEGIN
    SELECT
        p.product_id,
        p.image_path,
        p.name,
        c.category_name,
        p.cost_price,
        p.markup_percentage,
        p.stock,
        p.selling_price,
        p.barcode_path
    FROM
        products p
            JOIN
        categories c ON p.category_id = c.category_id;
END;

create procedure GetProductDiscounts()
BEGIN
    SELECT
        d.product_id,
        d.category_id,
        d.discount_type,
        d.discount_value,
        d.min_quantity,
        p.selling_price,
        p.category_id AS product_category_id
    FROM discounts d
             LEFT JOIN products p ON d.product_id = p.product_id OR d.category_id = p.category_id
    WHERE d.is_active = 1
      AND CURRENT_TIMESTAMP BETWEEN d.start_date AND d.end_date;
END;

create procedure add_sale_item(IN p_sale_id int, IN p_product_id int, IN p_quantity int, IN p_subtotal decimal(10, 2),
                               IN p_final_price decimal(10, 2))
BEGIN
    -- Insert the sale item
    INSERT INTO sales_items (sale_id, product_id, quantity, subtotal, final_price) 
    VALUES (p_sale_id, p_product_id, p_quantity, p_subtotal, p_final_price);
END;

create procedure process_sale(IN p_total_price decimal(10, 2), OUT p_sale_id int)
BEGIN
    -- Insert the sale record and get its ID
    INSERT INTO sales (total_price) VALUES (p_total_price);
    SET p_sale_id = LAST_INSERT_ID();
END;

create procedure sp_add_category(IN p_category_name varchar(100), OUT p_success tinyint(1))
BEGIN
    DECLARE EXIT HANDLER FOR 1062 -- Duplicate key error
        BEGIN
            SET p_success = FALSE;
        END;

    INSERT INTO categories (category_name) VALUES (p_category_name);
    SET p_success = TRUE;
END;

create procedure sp_delete_category(IN p_category_id int, OUT p_success tinyint(1))
BEGIN
    DECLARE EXIT HANDLER FOR 1451 -- Foreign key constraint error
        BEGIN
            SET p_success = FALSE;
        END;

    DELETE FROM categories WHERE category_id = p_category_id;

    IF ROW_COUNT() > 0 THEN
        SET p_success = TRUE;
    ELSE
        SET p_success = FALSE;
    END IF;
END;

create procedure sp_get_all_categories()
BEGIN
    SELECT category_id, category_name FROM categories ORDER BY category_id;
END;

create procedure sp_insert_discount(IN p_product_id int, IN p_category_id int,
                                    IN p_discount_type enum ('PERCENTAGE', 'FIXED', 'BOGO', 'BULK'),
                                    IN p_discount_value decimal(10, 2), IN p_min_quantity int, IN p_start_date datetime,
                                    IN p_end_date datetime, OUT p_success tinyint(1))
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            SET p_success = FALSE;
        END;

    INSERT INTO discounts (
        product_id,
        category_id,
        discount_type,
        discount_value,
        min_quantity,
        start_date,
        end_date,
        is_active
    ) VALUES (
                 p_product_id,
                 p_category_id,
                 p_discount_type,
                 p_discount_value,
                 p_min_quantity,
                 p_start_date,
                 p_end_date,
                 TRUE
             );

    SET p_success = TRUE;
END;

create procedure sp_insert_product(IN p_name varchar(255), IN p_category_id int, IN p_cost_price decimal(10, 2),
                                   IN p_markup_percentage decimal(10, 2), IN p_stock int,
                                   IN p_selling_price decimal(10, 2), IN p_image_path varchar(255),
                                   OUT p_product_id int)
BEGIN
    INSERT INTO products (
        name,
        category_id,
        cost_price,
        markup_percentage,
        stock,
        selling_price,
        image_path,
        last_restock
    )
    VALUES (
               p_name,
               p_category_id,
               p_cost_price,
               p_markup_percentage,
               p_stock,
               p_selling_price,
               IFNULL(p_image_path, ''),
               NOW()
           );

    SET p_product_id = LAST_INSERT_ID();
END;

create procedure sp_update_category(IN p_category_id int, IN p_category_name varchar(100), OUT p_success tinyint(1))
BEGIN
    DECLARE EXIT HANDLER FOR 1062 -- Duplicate key error
        BEGIN
            SET p_success = FALSE;
        END;

    UPDATE categories SET category_name = p_category_name
    WHERE category_id = p_category_id;

    IF ROW_COUNT() > 0 THEN
        SET p_success = TRUE;
    ELSE
        SET p_success = FALSE;
    END IF;
END;

create event update_discount_active_status on schedule
    every '1' DAY
        starts '2025-04-23 00:00:00'
    enable
    do
    BEGIN
        -- Activate discounts where start_date is reached and end_date is not passed
        UPDATE discounts
        SET is_active = 1
        WHERE start_date <= CURRENT_DATE
          AND end_date >= CURRENT_DATE
          AND is_active = 0;

        -- Deactivate discounts where end_date has passed
        UPDATE discounts
        SET is_active = 0
        WHERE end_date < CURRENT_DATE
          AND is_active = 1;
    END;

create event update_expired_discounts on schedule
    every '1' DAY
        starts '2025-04-23 00:00:00'
    enable
    do
    BEGIN
        UPDATE discounts
        SET is_active = FALSE
        WHERE end_date < NOW() AND is_active = TRUE;
    END;


