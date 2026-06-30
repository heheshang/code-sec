#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${1:-examples/benchmark/synthetic-100k}"
NUM_FILES=500

rm -rf "${OUTPUT_DIR:?}"

SRC_DIR="$OUTPUT_DIR/com/example/bench/svc"
mkdir -p "$SRC_DIR"

method_names=("handleRequest" "processData" "getResult" "execute" "fetch" "query" "load" "transform"
              "compute" "resolve" "find" "apply" "create" "update" "delete" "retrieve" "validate"
              "convert" "aggregate" "serialize")

sink_counter=0

for i in $(seq 1 $NUM_FILES); do
  num_methods=$((RANDOM % 7 + 16))
  class_name="Service${i}"

  has_rest=0
  if ((i % 10 == 0)); then
    has_rest=1
  fi

  sink_idx=$((sink_counter % 12))
  sink_counter=$((sink_counter + 1))

  {
    echo "package com.example.bench.svc;"
    echo ""

    if ((has_rest == 1)); then
      echo "import org.springframework.web.bind.annotation.*;"
    fi
    if ((sink_idx < 5)); then
      echo "import java.sql.Connection;"
      echo "import java.sql.Statement;"
    elif ((sink_idx < 8)); then
      echo "import java.sql.Connection;"
    elif ((sink_idx < 10)); then
      echo "import javax.crypto.Cipher;"
    elif ((sink_idx < 12)); then
      echo "import javax.servlet.http.HttpServletResponse;"
    fi

    if ((has_rest == 1)); then
      echo "@RestController"
    fi
    echo "public class ${class_name} {"
    echo ""

    for j in $(seq 1 $num_methods); do
      mname_idx=$((RANDOM % ${#method_names[@]}))
      mname="${method_names[$mname_idx]}_${j}"

      if ((has_rest == 1 && RANDOM % 3 == 0)); then
        echo "    @org.springframework.security.access.prepost.PreAuthorize(\"hasRole('USER')\")"
      fi
      if ((has_rest == 1 && RANDOM % 3 == 0)); then
        echo "    @GetMapping(\"/api/svc${i}/m${j}\")"
      fi

      arg_decl="String arg"
      if ((has_rest == 1 && RANDOM % 4 == 0)); then
        arg_decl="@RequestParam(\"input\") String arg"
      fi

      echo "    public String ${mname}(${arg_decl}) {"
      echo "        if (arg == null || arg.isEmpty()) { return \"empty\"; }"

      if ((j == 1)); then
        case $sink_idx in
          0|1|2|3|4)
            echo '        String query = "SELECT * FROM users WHERE id = " + arg;'
            echo '        java.sql.Connection conn = null;'
            echo '        java.sql.Statement stmt = conn.createStatement();'
            echo '        stmt.executeQuery(query);' ;;
          5|6|7)
            echo '        String password = "admin-secret-" + arg + "-pass!";'
            echo '        System.out.println("Using secret: " + password);' ;;
          8|9)
            echo '        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DES");'
            echo '        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, null);'
            echo '        byte[] encrypted = cipher.doFinal(arg.getBytes());' ;;
          10|11)
            echo '        String html = "<div>" + arg + "</div>";'
            echo '        System.out.println("XSS output: " + html);' ;;
        esac
      else
        echo "        System.out.println(\"Processing: \" + arg);"
      fi

      if ((has_rest == 1 && i > 1 && RANDOM % 3 == 0)); then
        prev=$((i - 1))
        echo "        new Service${prev}().${mname}(arg);"
      fi

      echo "        return \"ok_${i}_${j}\";"
      echo "    }"
      echo ""

      helper_count=$((RANDOM % 3 + 2))
      for k in $(seq 1 $helper_count); do
        echo "    private String helper_${j}_${k}(String in) { return \"h_${j}_${k}_\" + in; }"
      done
      echo ""
    done

    echo "    private String config_val_1 = \"default_val_${i}\";"
    echo "    private String config_val_2 = \"another_val_${i}\";"
    echo "    private int counter_${i} = 0;"
    echo "    private static final String VERSION_${i} = \"v1.0.${i}\";"

    if ((i > 1)); then
      prev=$((i - 1))
      echo "    private final Service${prev} dep_${i} = new Service${prev}();"
    fi

    echo "}"
  } > "${SRC_DIR}/Service${i}.java"
done

total_loc=$(find "$OUTPUT_DIR" -name '*.java' -exec cat {} + | wc -l | tr -d ' ')
echo "Generated $NUM_FILES Java files"
echo "Total LOC: $total_loc"
echo "Output: $SRC_DIR"
