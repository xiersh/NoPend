import os
import re

def process_file(filepath: str):
    defined_macros = set()
    conditional_macros = set()

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Merge continuation lines
    merged_lines = []
    current_line = []
    for line in content.splitlines():
        stripped = line.rstrip('\r\n')
        if stripped.rstrip().endswith('\\'):
            current_line.append(stripped.rstrip()[:-1])  # Remove line continuation character
        else:
            current_line.append(stripped)
            merged_lines.append(''.join(current_line))
            current_line = []
    if current_line:
        merged_lines.append(''.join(current_line))

    # Process comments
    processed_lines = []
    for line in merged_lines:
        line = re.sub(r'/\*.*?\*/', '', line)  # Remove block comments
        line = line.split('//', 1)[0].strip()  # Remove line comments
        processed_lines.append(line)

   # Macro name pattern (compatible with C99 standard)
    macro_pattern = r'[A-Za-z_][\w]*'
    
    for line in processed_lines:
        line = line.strip()
        if not line.startswith('#'):
            continue

        # Process define macros (keep unchanged)
        if re.match(r'^\s*#\s*define\s+', line, re.IGNORECASE):
            if match := re.match(rf'^\s*#\s*define\s+({macro_pattern})', line):
                defined_macros.add(match.group(1))
            continue

        # Process all conditional macro directives
        if match := re.match(r'^\s*#\s*(if|ifdef|ifndef|elif)\b', line, re.IGNORECASE):
            directive_type = match.group(1).lower()
            condition = line[match.end():].strip()

            # Process #ifdef/#ifndef
            if directive_type in ('ifdef', 'ifndef'):
                if macro_match := re.match(rf'\s*({macro_pattern})', condition):
                    conditional_macros.add(macro_match.group(1))
                continue

            # Process complex conditions (#if/#elif)
            # Match defined(...) or defined XXX
            defined_macros_in_line = re.findall(
                rf'!?\s*defined\s*(?:\(\s*)?({macro_pattern})',
                condition,
                flags=re.IGNORECASE
            )
            conditional_macros.update(defined_macros_in_line)

            # Match directly used macro names (e.g., !XXX)
            direct_macros = re.findall(
                rf'\b({macro_pattern})\b',
                re.sub(r'!+\s*', '', condition)  # Remove all ! and following spaces
            )
            conditional_macros.update(direct_macros)

    conditional_macros.discard('defined')
    conditional_macros.discard('IS_ENABLED')

    # print(f"filepath: {filepath}")
    # print(f"defined_macros: {defined_macros}")
    # print(f"conditional_macros: {conditional_macros}")    
    return defined_macros, conditional_macros

def generate_csv_for_file(src_file_path):
    csv_path = os.path.splitext(src_file_path)[0] + '-model' + '.csv'
    
    with open(csv_path, 'w', encoding='utf-8') as f:
        defined, cond = process_file(src_file_path)
        external_macros = cond
        cnt = 0
        f.write("parameter,name\n")
        for macro in external_macros:
            f.write(f"{cnt},{macro}\n")
            cnt += 1

def process_directory(target_dir):
    for root, dirs, files in os.walk(target_dir):
        for filename in files:
            if filename.lower().endswith(('.c', '.h')):
                src_path = os.path.join(root, filename)
                print(f"Processing: {src_path}")
                generate_csv_for_file(src_path)

if __name__ == "__main__":
    BUGS_DIRECTORY = 'bugs/'
    process_directory(BUGS_DIRECTORY)
    print("Processing completed")