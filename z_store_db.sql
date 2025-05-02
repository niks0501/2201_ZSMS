-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 02, 2025 at 11:35 AM
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
CREATE DEFINER=`root`@`localhost` PROCEDURE `add_sale_item` (IN `p_sale_id` INT, IN `p_product_id` INT, IN `p_quantity` INT, IN `p_subtotal` DECIMAL(10,2), IN `p_final_price` DECIMAL(10,2))   BEGIN
    -- Insert the sale item
    INSERT INTO sales_items (sale_id, product_id, quantity, subtotal, final_price) 
    VALUES (p_sale_id, p_product_id, p_quantity, p_subtotal, p_final_price);
END$$

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

CREATE DEFINER=`root`@`localhost` PROCEDURE `GetProductDiscounts` ()   BEGIN
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
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `process_sale` (IN `p_total_price` DECIMAL(10,2), OUT `p_sale_id` INT)   BEGIN
    -- Insert the sale record and get its ID
    INSERT INTO sales (total_price) VALUES (p_total_price);
    SET p_sale_id = LAST_INSERT_ID();
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_add_category` (IN `p_category_name` VARCHAR(100), OUT `p_success` TINYINT(1))   BEGIN
    DECLARE EXIT HANDLER FOR 1062 -- Duplicate key error
        BEGIN
            SET p_success = FALSE;
        END;

    INSERT INTO categories (category_name) VALUES (p_category_name);
    SET p_success = TRUE;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_delete_category` (IN `p_category_id` INT, OUT `p_success` TINYINT(1))   BEGIN
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

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_insert_discount` (IN `p_product_id` INT, IN `p_category_id` INT, IN `p_discount_type` ENUM('PERCENTAGE','FIXED','BOGO','BULK'), IN `p_discount_value` DECIMAL(10,2), IN `p_min_quantity` INT, IN `p_start_date` DATETIME, IN `p_end_date` DATETIME, OUT `p_success` TINYINT(1))   BEGIN
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

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_update_category` (IN `p_category_id` INT, IN `p_category_name` VARCHAR(100), OUT `p_success` TINYINT(1))   BEGIN
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
-- Table structure for table `credit_transactions`
--

CREATE TABLE `credit_transactions` (
  `transaction_id` int(11) NOT NULL,
  `customer_id` int(11) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `transaction_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('PAID','UNPAID') NOT NULL,
  `due_date` date DEFAULT NULL,
  `sale_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `credit_transactions`
--

INSERT INTO `credit_transactions` (`transaction_id`, `customer_id`, `amount`, `transaction_date`, `status`, `due_date`, `sale_id`) VALUES
(1, 2, 88.91, '2025-04-24 13:00:02', 'PAID', '2025-04-26', 13),
(2, 3, 53.60, '2025-04-24 14:17:06', 'PAID', '2025-05-01', 16),
(3, 4, 133.00, '2025-04-24 14:52:24', 'PAID', '2025-05-01', 19),
(4, 5, 72.11, '2025-04-24 15:17:20', 'PAID', '2025-05-01', 22),
(5, 6, 58.51, '2025-04-24 15:25:31', 'PAID', '2025-05-01', 23),
(6, 7, 58.11, '2025-04-24 15:31:09', 'UNPAID', '2025-05-01', 24),
(7, 8, 434.50, '2025-04-24 15:52:36', 'UNPAID', '2025-04-27', 29),
(8, 9, 48.60, '2025-04-24 15:56:11', 'UNPAID', '2025-04-27', 30),
(9, 10, 124.00, '2025-04-24 16:13:40', 'PAID', '2025-05-02', 32),
(10, 10, 45.85, '2025-04-24 16:16:26', 'PAID', '2025-05-02', 33),
(11, 10, 264.00, '2025-04-24 16:25:17', 'PAID', '2025-05-02', 34),
(12, 11, 105.60, '2025-04-26 03:21:33', 'UNPAID', '2025-04-27', 35),
(13, 12, 842.00, '2025-04-26 07:21:02', 'UNPAID', '2025-04-27', 37);

--
-- Triggers `credit_transactions`
--
DELIMITER $$
CREATE TRIGGER `update_credit_balance_after_payment` AFTER UPDATE ON `credit_transactions` FOR EACH ROW BEGIN
    -- Check if status was changed to 'PAID'
    IF OLD.status = 'UNPAID' AND NEW.status = 'PAID' THEN
        -- Subtract the transaction amount from customer's credit balance
        UPDATE customers
        SET credit_balance = credit_balance - NEW.amount
        WHERE customer_id = NEW.customer_id;
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `credit_transactions_view`
-- (See below for the actual view)
--
CREATE TABLE `credit_transactions_view` (
`transaction_id` int(11)
,`customer_name` varchar(100)
,`amount` decimal(10,2)
,`transaction_date` timestamp
,`due_date` date
,`status` enum('PAID','UNPAID')
);

-- --------------------------------------------------------

--
-- Table structure for table `customers`
--

CREATE TABLE `customers` (
  `customer_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `credit_balance` decimal(10,2) DEFAULT 0.00,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `customers`
--

INSERT INTO `customers` (`customer_id`, `name`, `credit_balance`, `phone`, `email`, `created_at`) VALUES
(2, 'Nikko Causapin', -88.91, NULL, NULL, '2025-04-24 13:00:02'),
(3, 'Lance Malata', 0.00, NULL, NULL, '2025-04-24 14:17:06'),
(4, 'Ley Vasquez', 0.00, NULL, NULL, '2025-04-24 14:52:24'),
(5, 'Allend Andaya', 0.00, '09342314522', 'allendandaya@gmail.com', '2025-04-24 15:17:20'),
(6, 'David Gludo', 0.00, '09345345345', 'davidgludo@gmail.com', '2025-04-24 15:25:31'),
(7, 'Marlo Condicion', 58.11, '09458641321', 'marlocondicion@gmail.com', '2025-04-24 15:31:09'),
(8, 'Cha Hae In', 434.50, '09897564121', 'nikkocausapin61@gmail.com', '2025-04-24 15:52:36'),
(9, 'Pierre Celso', 48.60, '', 'fapps761@gmail.com', '2025-04-24 15:56:11'),
(10, 'Charles Samontanez', -264.00, '', '', '2025-04-24 16:13:40'),
(11, 'Clinton John', 105.60, '0967057657567567', 'lancemalata82@gmail.com', '2025-04-26 03:21:33'),
(12, 'Marlo', 842.00, '0996855868475', '23-76558@g.batstate-u.edu.ph', '2025-04-26 07:21:02');

-- --------------------------------------------------------

--
-- Table structure for table `discounts`
--

CREATE TABLE `discounts` (
  `discount_id` int(11) NOT NULL,
  `product_id` int(11) DEFAULT NULL,
  `category_id` int(11) DEFAULT NULL,
  `discount_type` enum('PERCENTAGE','FIXED','BOGO','BULK') NOT NULL,
  `discount_value` decimal(10,2) NOT NULL,
  `min_quantity` int(11) DEFAULT 1,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `discounts`
--

INSERT INTO `discounts` (`discount_id`, `product_id`, `category_id`, `discount_type`, `discount_value`, `min_quantity`, `start_date`, `end_date`, `is_active`) VALUES
(1, 8, NULL, 'BULK', 10.00, 3, '2025-04-21 00:00:00', '2025-04-22 00:00:00', 0),
(2, NULL, 3, 'FIXED', 5.00, 1, '2025-04-21 00:00:00', '2025-04-23 00:00:00', 0),
(3, 2, NULL, 'BOGO', 15.00, 5, '2025-04-21 00:00:00', '2025-04-22 00:00:00', 0),
(4, NULL, 5, 'PERCENTAGE', 10.00, 1, '2025-04-21 00:00:00', '2025-04-24 00:00:00', 0),
(5, 43, NULL, 'FIXED', 10.00, 1, '2025-04-21 00:00:00', '2025-04-26 00:00:00', 1),
(6, NULL, 7, 'PERCENTAGE', 10.00, 1, '2025-04-21 00:00:00', '2025-04-30 00:00:00', 1),
(7, NULL, 9, 'BULK', 20.00, 5, '2025-04-21 00:00:00', '2025-04-28 00:00:00', 1),
(8, 12, NULL, 'PERCENTAGE', 10.00, 1, '2025-04-21 00:00:00', '2025-04-28 00:00:00', 1),
(9, NULL, 1, 'BOGO', 10.00, 2, '2025-04-21 00:00:00', '2025-04-25 00:00:00', 0);

--
-- Triggers `discounts`
--
DELIMITER $$
CREATE TRIGGER `set_discount_active_on_insert` BEFORE INSERT ON `discounts` FOR EACH ROW BEGIN
    IF NEW.start_date > CURRENT_DATE THEN
        SET NEW.is_active = 0;
    ELSEIF NEW.start_date <= CURRENT_DATE AND NEW.end_date >= CURRENT_DATE THEN
        SET NEW.is_active = 1;
    ELSE
        SET NEW.is_active = 0;
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `set_discount_active_on_update` BEFORE UPDATE ON `discounts` FOR EACH ROW BEGIN
    IF NEW.start_date > CURRENT_DATE THEN
        SET NEW.is_active = 0;
    ELSEIF NEW.start_date <= CURRENT_DATE AND NEW.end_date >= CURRENT_DATE THEN
        SET NEW.is_active = 1;
    ELSE
        SET NEW.is_active = 0;
    END IF;
END
$$
DELIMITER ;

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
(5, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744548726003.png', 'asdwdfdafASD', 1, 120.00, 10.00, 10, 132.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_5.png', '2025-04-13 12:52:06'),
(8, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744620861667.png', 'Nikko', 1, 120.00, 10.00, 10, 132.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_8.png', '2025-04-14 08:54:21'),
(9, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744638472320.png', 'dadadsdasdas', 1, 100.00, 10.00, 10, 110.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_9.png', '2025-04-14 13:47:52'),
(10, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647161056.png', 'weuiorhqwehpqwiond', 1, 20.00, 15.00, 10, 23.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_10.png', '2025-04-14 16:12:41'),
(11, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647260721.png', 'asd2asdasdasds', 1, 50.00, 5.00, 15, 52.50, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_11.png', '2025-04-14 16:14:20'),
(12, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647320897.png', 'blabala', 1, 500.00, 10.00, 10, 550.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_12.png', '2025-04-14 16:15:20'),
(13, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744647487040.png', 'quiyroosdfoi', 1, 327.00, 20.00, 10, 392.40, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_13.png', '2025-04-14 16:18:07'),
(14, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744708322000.png', 'qwoeipp[iqwe', 1, 130.00, 10.00, 10, 143.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_14.png', '2025-04-15 09:12:02'),
(15, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744708703617.png', 'sadasd2asd4235', 1, 400.00, 8.00, 10, 432.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_15.png', '2025-04-15 09:18:23'),
(16, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744711707141.png', 'AEDRQWEASASD', 1, 865.00, 5.00, 10, 908.25, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_16.png', '2025-04-15 10:08:27'),
(17, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744804202423.png', 'Bear Brand', 3, 25.00, 5.00, 10, 26.25, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_17.png', '2025-04-16 11:50:02'),
(18, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876239930.png', 'mklmvsdjksdjk', 4, 130.00, 5.00, 20, 136.50, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_18.png', '2025-04-17 07:50:39'),
(19, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876356847.png', 'tyuruerturt', 7, 600.00, 13.00, 20, 678.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_19.png', '2025-04-17 07:52:36'),
(20, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876572062.jpg', 'Surf 50 ML', 5, 13.00, 5.00, 30, 13.65, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_20.png', '2025-04-17 07:56:12'),
(21, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744876776539.jpg', 'Tide', 5, 12.00, 10.00, 30, 13.20, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_21.png', '2025-04-17 07:59:36'),
(23, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744877222046.png', 'Palmolive', 8, 30.00, 18.00, 10, 35.40, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_23.png', '2025-04-17 08:07:02'),
(24, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744877464451.png', 'erqerqerewrtwrt', 9, 45.00, 4.00, 10, 46.80, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_24.png', '2025-04-17 08:11:04'),
(25, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744878215564.png', 'ryuityiryee', 7, 53.00, 7.00, 20, 56.71, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_25.png', '2025-04-17 08:23:35'),
(42, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_1744881889420.png', '34t98wehfh489tb', 7, 50.00, 10.00, 30, 55.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_42.png', '2025-04-17 09:24:49'),
(43, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage\\product_43_1744898290811.png', 'selwyn', 4, 500.00, 12.00, 50, 560.00, 'C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\barcodes\\barcode_43.png', '2025-04-17 13:57:25');

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

--
-- Dumping data for table `sales`
--

INSERT INTO `sales` (`sale_id`, `total_price`, `sale_date`) VALUES
(1, 495.60, '2025-04-23 11:32:22'),
(2, 685.08, '2025-04-23 11:33:16'),
(3, 495.60, '2025-04-23 11:43:28'),
(4, 317.50, '2025-04-23 12:24:25'),
(5, 1793.61, '2025-04-23 16:03:03'),
(6, 713.59, '2025-04-24 12:31:09'),
(7, 62.25, '2025-04-24 12:36:07'),
(8, 727.24, '2025-04-24 12:38:29'),
(9, 655.00, '2025-04-24 12:45:47'),
(10, 661.71, '2025-04-24 12:48:17'),
(11, 95.40, '2025-04-24 12:55:21'),
(12, 264.00, '2025-04-24 12:56:12'),
(13, 227.82, '2025-04-24 12:59:13'),
(14, 390.00, '2025-04-24 14:08:15'),
(15, 13.20, '2025-04-24 14:13:55'),
(16, 103.60, '2025-04-24 14:16:25'),
(17, 120.00, '2025-04-24 14:29:11'),
(18, 264.00, '2025-04-24 14:44:31'),
(19, 133.00, '2025-04-24 14:51:02'),
(20, 30.00, '2025-04-24 15:00:39'),
(21, 942.40, '2025-04-24 15:04:19'),
(22, 0.00, '2025-04-24 15:16:27'),
(23, 0.00, '2025-04-24 15:24:03'),
(24, 34.00, '2025-04-24 15:30:24'),
(25, 1000.00, '2025-04-24 15:40:07'),
(26, 101.80, '2025-04-24 15:46:01'),
(27, 48.60, '2025-04-24 15:46:33'),
(28, 237.60, '2025-04-24 15:50:57'),
(29, 500.00, '2025-04-24 15:51:29'),
(30, 48.60, '2025-04-24 15:54:38'),
(31, 264.00, '2025-04-24 16:12:02'),
(32, 0.00, '2025-04-24 16:12:48'),
(33, 50.00, '2025-04-24 16:15:30'),
(34, 0.00, '2025-04-24 16:24:32'),
(35, 500.00, '2025-04-26 03:20:15'),
(36, 603.90, '2025-04-26 07:17:54'),
(37, 5.00, '2025-04-26 07:19:45'),
(38, 632.00, '2025-05-01 13:27:16'),
(39, 390.00, '2025-05-02 07:07:55');

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
-- Dumping data for table `sales_items`
--

INSERT INTO `sales_items` (`sale_item_id`, `sale_id`, `product_id`, `quantity`, `subtotal`, `final_price`) VALUES
(1, 1, 2, 2, 264.00, 105.60),
(2, 1, 3, 1, 126.00, 126.00),
(3, 1, 5, 1, 132.00, 132.00),
(4, 1, 8, 1, 132.00, 132.00),
(5, 2, 20, 1, 13.65, 10.92),
(6, 2, 21, 1, 13.20, 10.56),
(7, 2, 19, 1, 678.00, 474.60),
(8, 2, 18, 1, 136.50, 136.50),
(9, 2, 17, 2, 52.50, 52.50),
(10, 3, 2, 2, 264.00, 105.60),
(11, 3, 3, 1, 126.00, 126.00),
(12, 3, 5, 1, 132.00, 132.00),
(13, 3, 8, 1, 132.00, 132.00),
(14, 4, 8, 1, 132.00, 132.00),
(15, 4, 9, 1, 110.00, 110.00),
(16, 4, 10, 1, 23.00, 23.00),
(17, 4, 11, 1, 52.50, 52.50),
(18, 5, 2, 11, 1452.00, 659.93),
(19, 5, 3, 1, 126.00, 126.00),
(20, 5, 5, 1, 132.00, 132.00),
(21, 5, 8, 13, 1716.00, 765.68),
(22, 5, 9, 1, 110.00, 110.00),
(23, 6, 43, 1, 550.00, 539.99),
(24, 6, 42, 1, 55.00, 38.50),
(25, 6, 25, 1, 56.71, 39.70),
(26, 6, 24, 1, 46.80, 46.80),
(27, 6, 23, 1, 35.40, 35.40),
(28, 6, 21, 1, 13.20, 13.20),
(29, 7, 20, 1, 13.65, 13.65),
(30, 7, 21, 1, 13.20, 13.20),
(31, 7, 23, 1, 35.40, 35.40),
(32, 8, 21, 1, 13.20, 13.20),
(33, 8, 20, 1, 13.65, 13.65),
(34, 8, 23, 1, 35.40, 35.40),
(35, 8, 42, 1, 55.00, 38.50),
(36, 8, 43, 1, 550.00, 539.99),
(37, 8, 25, 1, 56.71, 39.70),
(38, 8, 24, 1, 46.80, 46.80),
(39, 9, 2, 1, 132.00, 132.00),
(40, 9, 3, 1, 126.00, 126.00),
(41, 9, 5, 1, 132.00, 132.00),
(42, 9, 8, 1, 132.00, 132.00),
(43, 9, 9, 1, 110.00, 110.00),
(44, 9, 10, 1, 23.00, 23.00),
(45, 10, 43, 1, 550.00, 550.00),
(46, 10, 42, 1, 55.00, 55.00),
(47, 10, 25, 1, 56.71, 56.71),
(48, 11, 24, 1, 46.80, 46.80),
(49, 11, 23, 1, 35.40, 35.40),
(50, 11, 21, 1, 13.20, 13.20),
(51, 12, 5, 1, 132.00, 132.00),
(52, 12, 8, 1, 132.00, 132.00),
(53, 13, 25, 1, 56.71, 56.71),
(54, 13, 24, 1, 46.80, 46.80),
(55, 13, 23, 1, 35.40, 35.40),
(56, 14, 3, 1, 126.00, 126.00),
(57, 14, 5, 1, 132.00, 132.00),
(58, 14, 8, 1, 132.00, 132.00),
(59, 15, 21, 1, 13.20, 13.20),
(60, 16, 42, 1, 55.00, 55.00),
(61, 16, 23, 1, 35.40, 35.40),
(62, 16, 21, 1, 13.20, 13.20),
(63, 17, 2, 2, 264.00, 105.60),
(64, 17, 9, 1, 110.00, 110.00),
(65, 18, 5, 1, 132.00, 132.00),
(66, 18, 8, 1, 132.00, 132.00),
(67, 19, 10, 1, 23.00, 23.00),
(68, 19, 9, 1, 110.00, 110.00),
(69, 20, 23, 1, 35.40, 35.40),
(70, 20, 21, 1, 13.20, 13.20),
(71, 20, 20, 1, 13.65, 13.65),
(72, 21, 13, 1, 392.40, 392.40),
(73, 21, 12, 1, 550.00, 550.00),
(74, 22, 25, 1, 56.71, 56.71),
(75, 22, 23, 1, 35.40, 35.40),
(76, 23, 24, 1, 46.80, 46.80),
(77, 23, 25, 1, 56.71, 56.71),
(78, 23, 42, 1, 55.00, 55.00),
(79, 24, 25, 1, 56.71, 56.71),
(80, 24, 23, 1, 35.40, 35.40),
(81, 25, 5, 1, 132.00, 132.00),
(82, 25, 8, 1, 132.00, 132.00),
(83, 25, 25, 1, 56.71, 56.71),
(84, 25, 43, 1, 550.00, 550.00),
(85, 26, 42, 1, 55.00, 55.00),
(86, 26, 24, 1, 46.80, 46.80),
(87, 27, 23, 1, 35.40, 35.40),
(88, 27, 21, 1, 13.20, 13.20),
(89, 28, 8, 1, 132.00, 132.00),
(90, 28, 2, 2, 264.00, 105.60),
(91, 29, 17, 1, 26.25, 26.25),
(92, 29, 16, 1, 908.25, 908.25),
(93, 30, 23, 1, 35.40, 35.40),
(94, 30, 21, 1, 13.20, 13.20),
(95, 31, 5, 1, 132.00, 132.00),
(96, 31, 8, 1, 132.00, 132.00),
(97, 32, 2, 2, 264.00, 105.60),
(98, 32, 10, 2, 46.00, 18.40),
(99, 33, 24, 1, 46.80, 46.80),
(100, 33, 23, 1, 35.40, 35.40),
(101, 33, 20, 1, 13.65, 13.65),
(102, 34, 2, 1, 132.00, 132.00),
(103, 34, 8, 1, 132.00, 132.00),
(104, 35, 8, 1, 132.00, 132.00),
(105, 35, 9, 1, 110.00, 110.00),
(106, 35, 2, 2, 264.00, 105.60),
(107, 35, 3, 1, 126.00, 126.00),
(108, 35, 5, 1, 132.00, 132.00),
(109, 36, 2, 1, 132.00, 132.00),
(110, 36, 3, 1, 126.00, 126.00),
(111, 36, 5, 1, 132.00, 132.00),
(112, 36, 8, 1, 132.00, 132.00),
(113, 36, 20, 6, 81.90, 81.90),
(114, 37, 5, 1, 132.00, 132.00),
(115, 37, 8, 1, 132.00, 132.00),
(116, 37, 10, 1, 23.00, 23.00),
(117, 37, 43, 1, 560.00, 560.00),
(118, 38, 2, 1, 132.00, 132.00),
(119, 38, 3, 1, 126.00, 126.00),
(120, 38, 5, 1, 132.00, 132.00),
(121, 38, 8, 1, 132.00, 132.00),
(122, 38, 9, 1, 110.00, 110.00),
(123, 39, 2, 1, 132.00, 132.00),
(124, 39, 3, 1, 126.00, 126.00),
(125, 39, 5, 1, 132.00, 132.00);

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
-- Indexes for table `credit_transactions`
--
ALTER TABLE `credit_transactions`
  ADD PRIMARY KEY (`transaction_id`),
  ADD KEY `customer_id` (`customer_id`);

--
-- Indexes for table `customers`
--
ALTER TABLE `customers`
  ADD PRIMARY KEY (`customer_id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Indexes for table `discounts`
--
ALTER TABLE `discounts`
  ADD PRIMARY KEY (`discount_id`),
  ADD KEY `product_id` (`product_id`),
  ADD KEY `category_id` (`category_id`);

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
  MODIFY `category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `credit_transactions`
--
ALTER TABLE `credit_transactions`
  MODIFY `transaction_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `customers`
--
ALTER TABLE `customers`
  MODIFY `customer_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `discounts`
--
ALTER TABLE `discounts`
  MODIFY `discount_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `low_stock_alerts`
--
ALTER TABLE `low_stock_alerts`
  MODIFY `alert_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `products`
--
ALTER TABLE `products`
  MODIFY `product_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=53;

--
-- AUTO_INCREMENT for table `product_prices`
--
ALTER TABLE `product_prices`
  MODIFY `price_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `sales`
--
ALTER TABLE `sales`
  MODIFY `sale_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;

--
-- AUTO_INCREMENT for table `sales_items`
--
ALTER TABLE `sales_items`
  MODIFY `sale_item_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=126;

-- --------------------------------------------------------

--
-- Structure for view `credit_transactions_view`
--
DROP TABLE IF EXISTS `credit_transactions_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `credit_transactions_view`  AS SELECT `ct`.`transaction_id` AS `transaction_id`, `c`.`name` AS `customer_name`, `ct`.`amount` AS `amount`, `ct`.`transaction_date` AS `transaction_date`, `ct`.`due_date` AS `due_date`, `ct`.`status` AS `status` FROM (`credit_transactions` `ct` join `customers` `c` on(`ct`.`customer_id` = `c`.`customer_id`)) ;

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
-- Constraints for table `credit_transactions`
--
ALTER TABLE `credit_transactions`
  ADD CONSTRAINT `credit_transactions_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE CASCADE;

--
-- Constraints for table `discounts`
--
ALTER TABLE `discounts`
  ADD CONSTRAINT `discounts_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  ADD CONSTRAINT `discounts_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`);

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

DELIMITER $$
--
-- Events
--
CREATE DEFINER=`root`@`localhost` EVENT `update_discount_active_status` ON SCHEDULE EVERY 1 DAY STARTS '2025-04-23 00:00:00' ON COMPLETION NOT PRESERVE ENABLE DO BEGIN
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
    END$$

CREATE DEFINER=`root`@`localhost` EVENT `update_expired_discounts` ON SCHEDULE EVERY 1 DAY STARTS '2025-04-23 00:00:00' ON COMPLETION NOT PRESERVE ENABLE DO BEGIN
        UPDATE discounts
        SET is_active = FALSE
        WHERE end_date < NOW() AND is_active = TRUE;
    END$$

DELIMITER ;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
