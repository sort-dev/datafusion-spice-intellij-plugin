-- Archived unexecuted draft, not a validated smoke-test suite.
-- The JSON ? operator below was rejected by the user's running Spice parser.
-- Kept only as a record of the aborted standalone test preparation.

-- smoke: sample
SELECT id, name, age
FROM spice.public.sample
ORDER BY id;

-- smoke: json_operators
SELECT
    '{"user":{"age":30}}' -> 'user' ->> 'age' AS age_text,
    '{"color":"black"}' ? 'color' AS has_color;

-- smoke: json_functions
SELECT
    json_get_int('{"user":{"age":30}}', 'user', 'age') AS age,
    json_get_str('{"user":{"name":"Ada"}}', 'user', 'name') AS name;

-- smoke: json_collections
SELECT
    json_length('[10,20,30]') AS item_count,
    json_object_keys('{"name":"Ada","age":30}') AS keys;

-- smoke: json_tree
SELECT fullkey, type, atom
FROM json_tree('{"a":[1,2]}')
WHERE atom IS NOT NULL
ORDER BY id;

-- smoke: qualify
SELECT name, row_number() OVER (ORDER BY age DESC) AS pos
FROM sample
QUALIFY pos = 1;

-- smoke: wildcard_replace
SELECT * REPLACE (upper(name) AS name)
FROM sample
ORDER BY id;

-- smoke: group_order_all
SELECT age >= 35 AS older, count(*) AS people
FROM sample
GROUP BY ALL
ORDER BY ALL;

-- smoke: arrays
SELECT
    array_sort([3, NULL, 1, 2], 'DESC', 'NULLS LAST') AS sorted,
    make_array(1,2,3) @> make_array(1,3) AS contains;

-- smoke: arrow_type
SELECT arrow_typeof(arrow_cast(1, 'UInt64')) AS native_type;

-- smoke: show_columns
SHOW COLUMNS FROM spice.public.sample;

-- smoke: optional_function_inventory
SELECT routine_name, function_type
FROM information_schema.routines
WHERE routine_name IN ('vector_search', 'text_search', 'rrf', 'rerank', 'ai_decide')
ORDER BY routine_name, function_type;
