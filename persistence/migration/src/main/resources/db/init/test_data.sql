-- liquibase formatted sql

-- changeset jakub:seed-test-reward-configs
-- Normal jackpot (1% chance)
INSERT INTO reward_config (id, type, chance)
VALUES (1, 'FIXED', 0.01);

-- Always win jackpot (100% chance)
INSERT INTO reward_config (id, type, chance)
VALUES (2, 'FIXED', 1.00);

-- Always lose jackpot (0% chance)
INSERT INTO reward_config (id, type, chance)
VALUES (3, 'FIXED', 0.00);

-- Reset the sequence to continue after our explicit IDs
SELECT setval('reward_config_id_seq', (SELECT MAX(id) FROM reward_config));

-- changeset jakub:seed-test-contribution-configs
-- Fixed 2% contribution rate
INSERT INTO contribution_config (id, type, percentage, min_percentage)
VALUES (1, 'FIXED', 0.02, 0.005);

-- Reset the sequence to continue after our explicit IDs
SELECT setval('contribution_config_id_seq', (SELECT MAX(id) FROM contribution_config));

-- changeset jakub:seed-test-jackpots
-- Jackpot 1: Normal 1% win chance
INSERT INTO jackpot (id, contribution_config_id, reward_config_id, initial_pool)
VALUES (1, 1, 1, 1000.00);

-- Jackpot 2: Always win (100% chance)
INSERT INTO jackpot (id, contribution_config_id, reward_config_id, initial_pool)
VALUES (2, 1, 2, 1000.00);

-- Jackpot 3: Always lose (0% chance)
INSERT INTO jackpot (id, contribution_config_id, reward_config_id, initial_pool)
VALUES (3, 1, 3, 1000.00);

-- Reset the sequence to continue after our explicit IDs
SELECT setval('jackpot_id_seq', (SELECT MAX(id) FROM jackpot));

-- changeset jakub:seed-test-contributions
-- Contribution for bet that will win (jackpot 2 has 100% chance)
INSERT INTO jackpot_contribution (
    type,
    bet_id,
    user_id,
    jackpot_id,
    stake_amount,
    contribution_amount,
    jackpot_amount_after
)
VALUES (
    'CONTRIBUTION',
    2,
    1,
    2,
    50.00,
    1.00, -- 2% of 50.00
    1001.00 -- initial_pool + contribution
);

-- Contribution for bet that will lose (jackpot 3 has 0% chance)
INSERT INTO jackpot_contribution (
    type,
    bet_id,
    user_id,
    jackpot_id,
    stake_amount,
    contribution_amount,
    jackpot_amount_after
)
VALUES (
    'CONTRIBUTION',
    3,
    1,
    3,
    50.00,
    1.00, -- 2% of 50.00
    1001.00 -- initial_pool + contribution
);

-- Reset the sequence
SELECT setval('jackpot_contribution_id_seq', (SELECT MAX(id) FROM jackpot_contribution));
