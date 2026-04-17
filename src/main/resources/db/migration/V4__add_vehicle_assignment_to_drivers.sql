ALTER TABLE drivers
ADD COLUMN IF NOT EXISTS vehicle_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_drivers_vehicle'
    ) THEN
        ALTER TABLE drivers
        ADD CONSTRAINT fk_drivers_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_drivers_vehicle_id
ON drivers(vehicle_id)
WHERE vehicle_id IS NOT NULL;
