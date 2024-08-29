import pandas as pd
import numpy as np
import os
import argparse

# Command-line argument parsing
parser = argparse.ArgumentParser(description="Generate test sets based on device difficulty.")
parser.add_argument('--num_devices', type=int, default=2, help='Total number of devices to include across both difficulty groups (easy and hard).')

args = parser.parse_args()
num_devices = args.num_devices

# Folder Paths
device_folder = "deviceFiles"
price_folder = "priceFiles"
solar_folder = "solarFiles"

os.makedirs("output_folder_a", exist_ok=True)
os.makedirs("output_folder_b", exist_ok=True)


price_files = os.listdir(price_folder)
solar_files = os.listdir(solar_folder)

# Choose random price file, and its corresponding solar file.
price_file = price_files[np.random.randint(len(price_files))]
solar_file = price_file

# Devices
device_files = os.listdir(device_folder)

devices_stats = []

for device_file in device_files:
    device_df = pd.read_csv(os.path.join(device_folder, device_file))
    
    # Calculate criteria
    duration = device_df['value'].count()         # Low duration is harder to plan
    peak = device_df['value'].max()               # Variability in peak needed so that different devices are used
    variability = device_df['value'].std()        # High variability so that lots of different device types are used
    
    # Store the results in a list along with the filename
    devices_stats.append({
        'filename': device_file,
        'duration': duration,
        'peak': peak,
        'variability': variability,
        'skewnness': device_df['value'].skew(),
        'kurtosis': device_df['value'].kurtosis()
    })

print(devices_stats)

# Step 4: Sort the devices based on different criteria
devices_stats_sorted_duration_asc = sorted(devices_stats, key=lambda x: x['variability'])       # Ascending order by duration
devices_stats_sorted_duration_desc = sorted(devices_stats, key=lambda x: x['variability'], reverse=True)  # Descending order by duration

devices_skewness_sorted = sorted(devices_stats, key=lambda x: x['skewnness'])

print("Devices sorted by skewness:")
for device in devices_skewness_sorted:
    print(device['filename'])

# Step 5: Create difficulty groups for both ascending (_a) and descending (_b) orders

# Print first num_devices device names of _asc
print("Devices in ascending order of duration:")
for i in range(num_devices):
    print(devices_stats_sorted_duration_asc[i]['filename'])


print ("Devices in descending order of duration:")
# Print first num_devices device names of _desc
for i in range(num_devices):
    print(devices_stats_sorted_duration_desc[i]['filename'])



print(f"Generated test sets for ascending and descending duration in the 'output_folder'.")
