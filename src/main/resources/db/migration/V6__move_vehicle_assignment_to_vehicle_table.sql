ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS driver_id BIGINT;

UPDATE vehicles v
SET driver_id = d.id
FROM drivers d
WHERE d.vehicle_id = v.id
  AND d.vehicle_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_vehicles_driver'
    ) THEN
        ALTER TABLE vehicles
        ADD CONSTRAINT fk_vehicles_driver
        FOREIGN KEY (driver_id) REFERENCES drivers(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_vehicles_driver_id
ON vehicles(driver_id);

DROP INDEX IF EXISTS ux_drivers_vehicle_id;

ALTER TABLE drivers
DROP CONSTRAINT IF EXISTS fk_drivers_vehicle;

ALTER TABLE drivers
DROP COLUMN IF EXISTS vehicle_id;
