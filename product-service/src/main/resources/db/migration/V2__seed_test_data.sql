-- Dev test data. Loaded through spring.flyway.locations in product-service.yml, NOT in tests.
INSERT INTO category (name, description) VALUES
    ('Keyboards',   'Computer Keyboards'),
    ('Monitors',    'Computer Monitors'),
    ('Screens',     'Display Screens'),
    ('Mice',        'Computer Mice'),
    ('Accessories', 'Computer Accessories');

-- one INSERT for all products; the category id is resolved by name through the JOIN
INSERT INTO product (name, description, price, available_quantity, category_id)
SELECT v.name, v.description, v.price, v.quantity, c.id
FROM (VALUES
    ('Mechanical Keyboard 1',          'Mechanical keyboard with RGB lighting',              99.99, 10, 'Keyboards'),
    ('Wireless Compact Keyboard 1',    'Wireless compact keyboard',                          79.99, 15, 'Keyboards'),
    ('Gaming Keyboard 1',              'Backlit gaming keyboard with customizable keys',    129.99, 20, 'Keyboards'),
    ('Ergonomic Keyboard 1',           'Mechanical keyboard with wrist rest',               109.99, 25, 'Keyboards'),
    ('Wireless Combo 1',               'Wireless keyboard and mouse combo',                  69.99, 18, 'Keyboards'),

    ('4K Monitor 1',                   '27-inch IPS monitor with 4K resolution',            399.99, 30, 'Monitors'),
    ('Ultra-wide Gaming Monitor 1',    'Ultra-wide gaming monitor with HDR support',        499.99, 25, 'Monitors'),
    ('Office Monitor 1',               '24-inch LED monitor for office use',                179.99, 22, 'Monitors'),
    ('Curved Monitor 1',               '32-inch curved monitor with AMD FreeSync',          329.99, 28, 'Monitors'),
    ('Portable Monitor 1',             'Portable USB-C monitor for laptops',                249.99, 35, 'Monitors'),

    ('Curved OLED Gaming Screen 1',    'Curved OLED gaming screen with 240Hz refresh rate', 799.99, 15, 'Screens'),
    ('QLED Monitor 1',                 'Flat QLED monitor with 1440p resolution',           599.99, 18, 'Screens'),
    ('Touch Screen Display 1',         '27-inch touch screen display for creative work',    699.99, 22, 'Screens'),
    ('Ultra-slim 4K HDR Display 1',    'Ultra-slim 4K HDR display for multimedia',          449.99, 20, 'Screens'),
    ('Gaming Projector 1',             'Gaming projector with low input lag',               899.99, 25, 'Screens'),

    ('RGB Gaming Mouse 1',             'Wireless gaming mouse with customizable RGB lighting', 59.99, 30, 'Mice'),
    ('Ergonomic Wired Mouse 1',        'Ergonomic wired mouse for productivity',             29.99, 28, 'Mice'),
    ('Ambidextrous Gaming Mouse 1',    'Ambidextrous gaming mouse with high DPI',            69.99, 32, 'Mice'),
    ('Travel Mouse 1',                 'Travel-sized compact mouse for laptops',             19.99, 26, 'Mice'),
    ('Vertical Ergonomic Mouse 1',     'Vertical ergonomic mouse for reduced strain',        39.99, 35, 'Mice'),

    ('Adjustable Laptop Stand 1',      'Adjustable laptop stand with cooling fan',           34.99, 25, 'Accessories'),
    ('Wireless Charging Pad 1',        'Wireless charging pad for smartphones',              24.99, 20, 'Accessories'),
    ('RGB Headset Stand 1',            'Gaming headset stand with RGB lighting',             49.99, 28, 'Accessories'),
    ('Bluetooth Keypad 1',             'Bluetooth mechanical keypad for tablets',            39.99, 22, 'Accessories'),
    ('External Hard Drive Enclosure 1','External hard drive enclosure with USB-C',            29.99, 30, 'Accessories')
) AS v(name, description, price, quantity, category_name)
JOIN category c ON c.name = v.category_name;