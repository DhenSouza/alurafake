import secrets
import base64

def generate_jwt_secret_key(num_bytes=64):
    """
    Gera uma chave secreta criptograficamente segura e a codifica em Base64.
    
    Args:
        num_bytes (int): O número de bytes para a chave antes da codificação.
                         Para HS256, use 32 bytes.
                         Para HS384, use 48 bytes.
                         Para HS512, use 64 bytes (recomendado para maior segurança).
    
    Returns:
        str: A chave secreta codificada em Base64.
    """
    random_bytes = secrets.token_bytes(num_bytes)
    secret_key_base64 = base64.b64encode(random_bytes).decode('utf-8')
    return secret_key_base64

if __name__ == "__main__":
    # Gerar uma chave para HS512 (64 bytes = 512 bits)
    # Este é um bom padrão de segurança.
    chave_secreta_hs512 = generate_jwt_secret_key(64)
    
    print("Sua chave secreta JWT gerada (Base64 encoded para HS512):")
    print(chave_secreta_hs512)
    print(f"Comprimento da chave Base64: {len(chave_secreta_hs512)} caracteres") # Para 64 bytes, será 88 caracteres em Base64
    print("\nLembre-se:")
    print("1. Guarde esta chave em um local MUITO seguro.")
    print("2. Use esta chave no seu arquivo 'application.properties' para a propriedade 'jwt.secret'.")
    print("3. NÃO coloque esta chave diretamente no seu código ou em sistemas de controle de versão se não for devidamente protegida (ex: usando variáveis de ambiente ou cofres de segredos em produção).")
    print("4. Gere uma chave diferente para cada ambiente (desenvolvimento, homologação, produção).")

    # Exemplo para HS256 (32 bytes = 256 bits), se precisar de uma menor (menos seguro que HS512)
    # chave_secreta_hs256 = generate_jwt_secret_key(32)
    # print("\nExemplo de chave para HS256 (Base64 encoded):")
    # print(chave_secreta_hs256)
    # print(f"Comprimento da chave Base64: {len(chave_secreta_hs256)} caracteres") # Para 32 bytes, será 44 caracteres