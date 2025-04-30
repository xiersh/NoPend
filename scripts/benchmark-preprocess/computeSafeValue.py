import os
import csv

def compute_safe_values(mfs_csv_path):
    mfs_data = []


    with open(mfs_csv_path, newline='', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            if row and any(cell.strip() for cell in row):
                mfs_data.append(row)

    if not mfs_data:
        return None

    num_columns = max(len(row) for row in mfs_data)
    safe_values = []

    for col in range(num_columns):
        values = set()
        for row in mfs_data:
            if col < len(row):
                val = row[col].strip()
                if val in {"0", "1"}:
                    values.add(val)


        if "1" not in values and "0" not in values:
            safe_values.append("-")
        elif "1" not in values:
            safe_values.append("1")
        elif "0" not in values:
            safe_values.append("0")
        else:
            return None

    return safe_values

def main():
    root_dir = "bugs"
    missing_safe_values = []


    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith("-mfs.csv"):
                mfs_csv_path = os.path.join(dirpath, filename)
                safe_csv_path = os.path.join(dirpath, filename.replace("-mfs.csv", "-safe.csv"))


                safe_values = compute_safe_values(mfs_csv_path)

                if safe_values:

                    with open(safe_csv_path, 'w', newline='', encoding='utf-8') as f:
                        writer = csv.writer(f)
                        writer.writerow(safe_values)
                else:

                    missing_safe_values.append(mfs_csv_path)


    if missing_safe_values:
        print("no safe value for the following files: ")
        for path in missing_safe_values:
            print(path)

if __name__ == "__main__":
    main()
