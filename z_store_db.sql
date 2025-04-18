-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Apr 17, 2025 at 06:14 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `z_store_db`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `GetAllProducts` ()   BEGIN
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
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_add_category` (IN `p_category_name` VARCHAR(100), OUT `p_success` BOOLEAN)   BEGIN
    DECLARE EXIT HANDLER FOR 1062 -- Duplicate key error
        BEGIN
            SET p_success = FALSE;
        END;

    INSERT INTO categories (category_name) VALUES (p_category_name);
    SET p_success = TRUE;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_delete_category` (IN `p_category_id` INT, OUT `p_success` BOOLEAN)   BEGIN
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
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_get_all_categories` ()   BEGIN
    SELECT category_id, category_name FROM categories ORDER BY category_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_insert_product` (IN `p_name` VARCHAR(255), IN `p_category_id` INT, IN `p_cost_price` DECIMAL(10,2), IN `p_markup_percentage` DECIMAL(10,2), IN `p_stock` INT, IN `p_selling_price` DECIMAL(10,2), IN `p_image_path` VARCHAR(255), OUT `p_product_id` INT)   BEGIN
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
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_update_category` (IN `p_category_id` INT, IN `p_category_name` VARCHAR(100), OUT `p_success` BOOLEAN)   BEGIN
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
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `admin`
--

CREATE TABLE `admin` (
  `admin_id` int(11) NOT NULL,
  `full_name` varchar(50) DEFAULT NULL,
  `username` varchar(20) DEFAULT NULL,
  `password` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `admin`
--

INSERT INTO `admin` (`admin_id`, `full_name`, `username`, `password`) VALUES
(1, 'Zeny Anzaldo', 'zeny', '1234');

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `category_id` int(11) NOT NULL,
  `category_name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`category_id`, `category_name`) VALUES
(7, 'Appliances'),
(4, 'Bath'),
(9, 'Cottons'),
(3, 'Dairy'),
(5, 'Detergents'),
(1, 'Product'),
(8, 'Soapy');

-- --------------------------------------------------------

--
-- Stand-in structure for view `lowstockproducts`
-- (See below for the actual view)
--
CREATE TABLE `lowstockproducts` (
`product_id` int(11)
,`name` varchar(200)
,`stock` int(11)
);

-- --------------------------------------------------------

--
-- Table structure for table `low_stock_alerts`
--

CREATE TABLE `low_stock_alerts` (
  `alert_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `stock_level` int(11) NOT NULL,
  `alert_date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `products`
--

CREATE TABLE `products` (
  `product_id` int(11) NOT NULL,
  `image_path` varchar(255) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `category_id` int(11) NOT NULL,
  `cost_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `markup_percentage` decimal(5,2) NOT NULL,
  `stock` int(11) NOT NULL DEFAULT 0,
  `selling_price` decimal(10,2) NOT NULL,
  `barcode_path` varchar(255) DEFAULT NULL,
  `last_restock` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `products`
--

INSERT INTO `products` (`product_id`, `image_path`, `name`, `category_id`, `cost_price`, `markup_percentage`, `stock`, `selling_price`, `barcode_path`, `last_restock`) VALUES
(2, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744444441957.png', 'fkenfjsdbfkjsbf', 1, 120.00, 10.00, 10, 132.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_2.png', '2025-04-12 07:54:01'),
(3, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744446779975.png', 'zdfdxzx', 1, 120.00, 5.00, 12, 126.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_3.png', '2025-04-12 08:32:59'),
(4, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744446942339.png', 'qasdfghjkl', 1, -9.00, 90.00, 13, -17.10, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_4.png', '2025-04-12 08:35:42'),
(5, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744548726003.png', 'asdwdfdafASD', 1, 120.00, 10.00, 10, 132.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_5.png', '2025-04-13 12:52:06'),
(8, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744620861667.png', 'Nikko', 1, 120.00, 10.00, 10, 132.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_8.png', '2025-04-14 08:54:21'),
(9, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744638472320.png', 'dadadsdasdas', 1, 100.00, 10.00, 10, 110.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_9.png', '2025-04-14 13:47:52'),
(10, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647161056.png', 'weuiorhqwehpqwiond', 1, 20.00, 15.00, 10, 23.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_10.png', '2025-04-14 16:12:41'),
(11, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647260721.png', 'asd2asdasdasds', 1, 50.00, 5.00, 10, 52.50, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_11.png', '2025-04-14 16:14:20'),
(12, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647320897.png', 'blabala', 1, 500.00, 10.00, 10, 550.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_12.png', '2025-04-14 16:15:20'),
(13, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647487040.png', 'quiyroosdfoi', 1, 327.00, 20.00, 10, 392.40, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_13.png', '2025-04-14 16:18:07'),
(14, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744708322000.png', 'qwoeipp[iqwe', 1, 130.00, 10.00, 10, 143.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_14.png', '2025-04-15 09:12:02'),
(15, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744708703617.png', 'sadasd2asd4235', 1, 400.00, 8.00, 10, 432.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_15.png', '2025-04-15 09:18:23'),
(16, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744711707141.png', 'AEDRQWEASASD', 1, 865.00, 5.00, 10, 908.25, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_16.png', '2025-04-15 10:08:27'),
(17, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744804202423.png', 'Bear Brand', 3, 25.00, 5.00, 10, 26.25, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_17.png', '2025-04-16 11:50:02'),
(18, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876239930.png', 'mklmvsdjksdjk', 4, 130.00, 5.00, 20, 136.50, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_18.png', '2025-04-17 07:50:39'),
(19, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876356847.png', 'tyuruerturt', 7, 600.00, 13.00, 10, 678.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_19.png', '2025-04-17 07:52:36'),
(20, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876572062.jpg', 'Surf 50 ML', 5, 13.00, 5.00, 20, 13.65, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_20.png', '2025-04-17 07:56:12'),
(21, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876776539.jpg', 'Tide', 5, 12.00, 10.00, 30, 13.20, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_21.png', '2025-04-17 07:59:36'),
(22, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876990034.png', 'Safe Guard', 8, 34.00, 15.00, 10, 39.10, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_22.png', '2025-04-17 08:03:10'),
(23, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744877222046.png', 'Palmolive', 8, 30.00, 18.00, 10, 35.40, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_23.png', '2025-04-17 08:07:02'),
(24, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744877464451.png', 'erqerqerewrtwrt', 9, 45.00, 4.00, 10, 46.80, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_24.png', '2025-04-17 08:11:04'),
(25, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744878215564.png', 'ryuityiryee', 7, 53.00, 7.00, 10, 56.71, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_25.png', '2025-04-17 08:23:35'),
(28, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744878768378.png', 'awasdawdawdasdad', 4, 23.00, 5.00, 20, 24.15, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_28.png', '2025-04-17 08:32:48'),
(33, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\src\\main\\resources\\productImage\\product_33_1744898076858.png', '90tyegdfggad', 5, 45.00, 12.00, 35, 50.40, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_33.png', '2025-04-17 09:04:50'),
(42, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744881889420.png', '34t98wehfh489tb', 7, 50.00, 10.00, 20, 55.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_42.png', '2025-04-17 09:24:49'),
(43, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_43_1744898290811.png', 'selwyn', 4, 1200.00, 2.00, 10, 1224.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_43.png', '2025-04-17 13:57:25'),
(44, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_44_1744904932071.png', 'Mouses', 5, 500.00, 10.00, 10, 550.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_44.png', '2025-04-17 14:02:06');

-- --------------------------------------------------------

--
-- Table structure for table `product_prices`
--

CREATE TABLE `product_prices` (
  `price_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `old_price` decimal(10,2) NOT NULL,
  `new_price` decimal(10,2) NOT NULL,
  `change_date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `sales`
--

CREATE TABLE `sales` (
  `sale_id` int(11) NOT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `sale_date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `sales_items`
--

CREATE TABLE `sales_items` (
  `sale_item_id` int(11) NOT NULL,
  `sale_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `final_price` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admin`
--
ALTER TABLE `admin`
  ADD PRIMARY KEY (`admin_id`);

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `category_name` (`category_name`);

--
-- Indexes for table `low_stock_alerts`
--
ALTER TABLE `low_stock_alerts`
  ADD PRIMARY KEY (`alert_id`),
  ADD KEY `product_id` (`product_id`);

--
-- Indexes for table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`product_id`),
  ADD UNIQUE KEY `barcode_path` (`barcode_path`),
  ADD KEY `category_id` (`category_id`);

--
-- Indexes for table `product_prices`
--
ALTER TABLE `product_prices`
  ADD PRIMARY KEY (`price_id`),
  ADD KEY `product_id` (`product_id`);

--
-- Indexes for table `sales`
--
ALTER TABLE `sales`
  ADD PRIMARY KEY (`sale_id`);

--
-- Indexes for table `sales_items`
--
ALTER TABLE `sales_items`
  ADD PRIMARY KEY (`sale_item_id`),
  ADD KEY `sale_id` (`sale_id`),
  ADD KEY `product_id` (`product_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `admin`
--
ALTER TABLE `admin`
  MODIFY `admin_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `categories`
--
ALTER TABLE `categories`
  MODIFY `category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `low_stock_alerts`
--
ALTER TABLE `low_stock_alerts`
  MODIFY `alert_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `products`
--
ALTER TABLE `products`
  MODIFY `product_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=46;

--
-- AUTO_INCREMENT for table `product_prices`
--
ALTER TABLE `product_prices`
  MODIFY `price_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `sales`
--
ALTER TABLE `sales`
  MODIFY `sale_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `sales_items`
--
ALTER TABLE `sales_items`
  MODIFY `sale_item_id` int(11) NOT NULL AUTO_INCREMENT;

-- --------------------------------------------------------

--
-- Structure for view `lowstockproducts`
--
DROP TABLE IF EXISTS `lowstockproducts`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `lowstockproducts`  AS SELECT `p`.`product_id` AS `product_id`, `p`.`name` AS `name`, `p`.`stock` AS `stock` FROM `products` AS `p` WHERE `p`.`stock` <= 5 ;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `low_stock_alerts`
--
ALTER TABLE `low_stock_alerts`
  ADD CONSTRAINT `low_stock_alerts_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE;

--
-- Constraints for table `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `products_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE CASCADE;

--
-- Constraints for table `product_prices`
--
ALTER TABLE `product_prices`
  ADD CONSTRAINT `product_prices_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE;

--
-- Constraints for table `sales_items`
--
ALTER TABLE `sales_items`
  ADD CONSTRAINT `sales_items_ibfk_1` FOREIGN KEY (`sale_id`) REFERENCES `sales` (`sale_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `sales_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
